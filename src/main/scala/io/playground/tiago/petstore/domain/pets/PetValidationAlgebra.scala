package io.playground.tiago.petstore.domain.pets

import io.playground.tiago.petstore.domain.{
  PetAlreadyExistsError,
  PetNotFoundError
}

import cats.data.EitherT

trait PetValidationAlgebra[F[_]] {
  def doesNotExist(pet: Pet): EitherT[F, PetAlreadyExistsError, Unit]
  
  def exists(petId: Option[Long]): EitherT[F, PetNotFoundError.type, Unit]
}