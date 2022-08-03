package io.playground.tiago.petstore.domain.users

import io.playground.tiago.petstore.domain.{
  UserAlreadyExistsError,
  UserNotFoundError
}

import cats.data._
import cats.Functor
import cats.Monad
import cats.syntax.functor._

class UserService[F[_]](
    repository: UserRepositoryAlgebra[F],
    validation: UserValidationAlgebra[F]
) {

  def create(
      user: User
  )(implicit M: Monad[F]): EitherT[F, UserAlreadyExistsError, User] =
    for {
      _ <- validation.doesNotExist(user)
      saved <- EitherT.liftF(repository.create(user))
    } yield saved

  def update(
      user: User
  )(implicit M: Monad[F]): EitherT[F, UserNotFoundError.type, User] =
    for {
      _ <- validation.exists(user.id)
      saved <- repository.update(user).toRight(UserNotFoundError)
    } yield saved

  def delete(userId: Long)(implicit F: Functor[F]): F[Unit] =
    repository.delete(userId).value.void

  def deleteByName(userName: String)(implicit F: Functor[F]): F[Unit] =
    repository.deleteByName(userName).value.void

  def get(userId: Long)(implicit
      F: Functor[F]
  ): EitherT[F, UserNotFoundError.type, User] =
    repository.get(userId).toRight(UserNotFoundError)

  def getByName(
      userName: String
  )(implicit F: Functor[F]): EitherT[F, UserNotFoundError.type, User] =
    repository.getByName(userName).toRight(UserNotFoundError)

  def list(pageSize: Int, offset: Int): F[List[User]] =
    repository.list(pageSize, offset)
}

object UserService {
  def apply[F[_]](
      repository: UserRepositoryAlgebra[F],
      validation: UserValidationAlgebra[F]
  ): UserService[F] =
    new UserService[F](repository, validation)
}
