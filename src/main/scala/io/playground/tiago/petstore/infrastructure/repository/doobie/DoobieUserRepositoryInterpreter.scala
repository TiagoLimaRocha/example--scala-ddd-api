package io.playground.tiago.petstore.infrastructure.repository.doobie

import io.playground.tiago.petstore.domain.users.{
  Role,
  User,
  UserRepositoryAlgebra
}
import io.playground.tiago.petstore.infrastructure.repository.doobie.SQLPagination._

import cats.data.OptionT
import cats.effect.Bracket
import cats.syntax.all._

import doobie._
import doobie.implicits._

import io.circe.parser.decode
import io.circe.syntax._

import tsec.authentication.IdentityStore

private object UserSQL {
  implicit val roleMeta: Meta[Role] =
    Meta[String].imap(decode[Role](_).leftMap(throw _).merge)(_.asJson.toString)

  def insert(user: User): Update0 = sql"""
    INSERT INTO USERS 
      (
        USER_NAME, 
        FIRST_NAME, 
        LAST_NAME, 
        EMAIL, 
        HASH, 
        PHONE, 
        ROLE
      )
    VALUES (
      ${user.userName},
      ${user.firstName},
      ${user.lastName},
      ${user.email},
      ${user.hash},
      ${user.phone},
      ${user.role})
  """.update

  def update(user: User, id: Long): Update0 = sql"""
    UPDATE USERS
    SET 
      FIRST_NAME = ${user.firstName}, 
      LAST_NAME = ${user.lastName},
      EMAIL = ${user.email}, 
      HASH = ${user.hash}, 
      PHONE = ${user.phone}, 
      ROLE = ${user.role}
    WHERE ID = $id
  """.update

  def delete(userId: Long): Update0 = sql"""
    DELETE FROM USERS WHERE ID = $userId
  """.update

  def select(userId: Long): Query0[User] = sql"""
    SELECT * FROM USERS WHERE ID = $userId
  """.query

  def selectAll: Query0[User] = sql"""
    SELECT * FROM USERS
  """.query

  def selectByName(userName: String): Query0[User] = sql"""
    SELECT * FROM USERS WHERE USER_NAME = $userName
  """.query[User]
}

class DoobieUserRepositoryInterpreter[F[_]: Bracket[*[_], Throwable]](
    val xa: Transactor[F]
) extends UserRepositoryAlgebra[F]
    with IdentityStore[F, Long, User] { self =>

  def create(user: User): F[User] =
    UserSQL
      .insert(user)
      .withUniqueGeneratedKeys[Long]("ID")
      .map(id => user.copy(id = id.some))
      .transact(xa)

  def update(user: User): OptionT[F, User] =
    OptionT
      .fromOption[F](user.id)
      .semiflatMap(id =>
        UserSQL
          .update(user, id)
          .run
          .transact(xa)
          .as(user)
      )

  def get(userId: Long): OptionT[F, User] = OptionT(
    UserSQL.select(userId).option.transact(xa)
  )

  def getByName(userName: String): OptionT[F, User] =
    OptionT(
      UserSQL
        .selectByName(userName)
        .option
        .transact(xa)
    )

  def delete(userId: Long): OptionT[F, User] =
    get(userId).semiflatMap(user =>
      UserSQL
        .delete(userId)
        .run
        .transact(xa)
        .as(user)
    )

  def deleteByName(userName: String): OptionT[F, User] =
    getByName(userName)
      .mapFilter(_.id)
      .flatMap(delete)

  def list(pageSize: Int, offset: Int): F[List[User]] =
    paginate(pageSize, offset)(UserSQL.selectAll)
      .to[List]
      .transact(xa)
}

object DoobieUserRepositoryInterpreter {
  def apply[F[_]: Bracket[*[_], Throwable]](
      xa: Transactor[F]
  ): DoobieUserRepositoryInterpreter[F] =
    new DoobieUserRepositoryInterpreter(xa)
}
