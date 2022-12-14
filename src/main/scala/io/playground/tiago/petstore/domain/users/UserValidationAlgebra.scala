package io.playground.tiago.petstore.domain.users

import io.playground.tiago.petstore.domain.{
  UserAlreadyExistsError,
  UserNotFoundError
}

import cats.data.EitherT

trait UserValidationAlgebra[F[_]] {
  def doesNotExist(user: User): EitherT[F, UserAlreadyExistsError, Unit]

  def exists(userId: Option[Long]): EitherT[F, UserNotFoundError.type, Unit]
}
