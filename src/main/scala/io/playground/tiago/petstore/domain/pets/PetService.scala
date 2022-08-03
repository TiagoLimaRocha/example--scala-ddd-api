package io.playground.tiago.petstore.domain.pets

import io.playground.tiago.petstore.domain.{
  PetNotFoundError,
  PetAlreadyExistsError
}

import cats.Functor
import cats.data._
import cats.Monad
import cats.syntax.all._

class PetService[F[_]](
    repository: PetRepositoryAlgebra[F],
    validation: PetValidationAlgebra[F]
) {
  def create(
      pet: Pet
  )(implicit Monad: Monad[F]): EitherT[F, PetAlreadyExistsError, Pet] =
    for {
      _ <- validation.doesNotExist(pet)
      saved <- EitherT.liftF(repository.create(pet))
    } yield saved

  def update(
      pet: Pet
  )(implicit Modnad: Monad[F]): EitherT[F, PetNotFoundError.type, Pet] =
    for {
      _ <- validation.exists(pet.id)
      saved <- EitherT.fromOptionF(repository.update(pet), PetNotFoundError)
    } yield saved

  def delete(id: Long)(implicit F: Functor[F]): F[Unit] =
    repository.delete(id).as(())

  def get(id: Long)(implicit
      F: Functor[F]
  ): EitherT[F, PetNotFoundError.type, Pet] =
    EitherT.fromOptionF(repository.get(id), PetNotFoundError)

  def getByStatus(status: NonEmptyList[PetStatus]): F[List[Pet]] =
    repository.getByStatus(status)

  def getByTag(tags: NonEmptyList[String]): F[List[Pet]] =
    repository.getByTag(tags)

  def list(pageSize: Int, offset: Int): F[List[Pet]] =
    repository.list(pageSize, offset)
}

object PetService {
  def apply[F[_]](
      repository: PetRepositoryAlgebra[F],
      validation: PetValidationAlgebra[F]
  ): PetService[F] =
    new PetService[F](repository, validation)
}
