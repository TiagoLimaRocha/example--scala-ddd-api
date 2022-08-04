package io.playground.tiago.petstore

import io.playground.tiago.petstore.config._
import io.playground.tiago.petstore.domain.users._
import io.playground.tiago.petstore.domain.pets._
import io.playground.tiago.petstore.infrastructure.endpoint._
import io.playground.tiago.petstore.domain.authentication.Auth
import io.playground.tiago.petstore.infrastructure.repository.doobie.{
  DoobieAuthRepositoryInterpreter,
  DoobieUserRepositoryInterpreter,
  DoobiePetRepositoryInterpreter
}

import cats.effect._
import io.circe.config.parser
import doobie.util.ExecutionContexts

import org.http4s.server.{Router, Server => H4Server}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._

import tsec.passwordhashers.jca.BCrypt
import tsec.authentication.SecuredRequestHandler
import tsec.mac.jca.HMACSHA256

object Server extends IOApp {
  def createServer[F[_]: ContextShift: ConcurrentEffect: Timer]
      : Resource[F, H4Server[F]] =
    for {
      conf <- Resource.eval(parser.decodePathF[F, PetStoreConfig]("petstore"))

      serverEc <- ExecutionContexts.cachedThreadPool[F]
      connEc <- ExecutionContexts.fixedThreadPool[F](
        conf.db.connections.poolSize
      )
      txnEc <- ExecutionContexts.cachedThreadPool[F]

      xa <- DatabaseConfig.dbTransactor(
        conf.db,
        connEc,
        Blocker.liftExecutionContext(txnEc)
      )

      key <- Resource.eval(HMACSHA256.generateKey[F])

      authRepo = DoobieAuthRepositoryInterpreter[F, HMACSHA256](key, xa)

      userRepo = DoobieUserRepositoryInterpreter[F](xa)
      userValidation = UserValidationInterpreter[F](userRepo)
      userService = UserService[F](userRepo, userValidation)

      petRepo = DoobiePetRepositoryInterpreter[F](xa)
      petValidation = PetValidationInterpreter[F](petRepo)
      petService = PetService[F](petRepo, petValidation)

      authenticator = Auth.jwtAuthenticator[F, HMACSHA256](
        key,
        authRepo,
        userRepo
      )

      routeAuth = SecuredRequestHandler(authenticator)

      httpApp = Router(
        "/users" -> UserEndpoints
          .endpoints[F, BCrypt, HMACSHA256](
            userService,
            BCrypt.syncPasswordHasher[F],
            routeAuth
          ),
        "/pets" -> PetEndpoints.endpoints[F, HMACSHA256](petService, routeAuth)
      ).orNotFound

      _ <- Resource.eval(DatabaseConfig.initializeDb(conf.db))

      server <- BlazeServerBuilder[F](serverEc)
        .bindHttp(conf.server.port, conf.server.host)
        .withHttpApp(httpApp)
        .resource
    } yield server

  def run(args: List[String]): IO[ExitCode] =
    createServer.use(_ => IO.never).as(ExitCode.Success)
}
