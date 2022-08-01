package io.playground.tiago.petstore.domain.pets

import io.playground.tiago.petstore.domain.{
  PetAlreadyExistsError,
  PetNotFoundError
}

import cats.Applicative
import cats.data.EitherT
import cats.syntax.all._

class PetValidationInterpreter[F[_]: Applicative](
    repo: PetRepositoryAlgebra[F]
) extends PetValidationAlgebra[F] {
  def doesNotExist(pet: Pet): EitherT[F, PetAlreadyExistsError, Unit] =
    EitherT {
      repo.getByNameAndCategory(pet.name, pet.category).map { matches =>
        if (matches.forall(possibleMatch => possibleMatch.bio != pet.bio)) {
          Right(())
        } else {
          Left(PetAlreadyExistsError(pet))
        }
      }
    }

  def exists(petId: Option[Long]): EitherT[F, PetNotFoundError.type, Unit] =
    EitherT {
      petId match {
        case Some(id) =>
          repo.get(id).map {
            case Some(_) => Right(())
            case None    => Left(PetNotFoundError)
          }
        case None =>
          Either.left[PetNotFoundError.type, Unit](PetNotFoundError).pure[F]
      }
    }

}

object PetValidationInterpreter {
  def apply[F[_]: Applicative](
      repo: PetRepositoryAlgebra[F]
  ): PetValidationAlgebra[F] =
    new PetValidationInterpreter[F](repo)
}
