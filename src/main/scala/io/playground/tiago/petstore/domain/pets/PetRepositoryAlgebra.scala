package io.playground.tiago.petstore.domain.pets

import cats.data.NonEmptyList

trait PetRepositoryAlgebra[F[_]] {
  def create(pet: Pet): F[Pet]

  def update(pet: Pet): F[Option[Pet]]

  def delete(id: Long): F[Option[Pet]]

  def get(id: Long): F[Option[Pet]]

  def getByNameAndCategory(name: String, category: String): F[Set[Pet]]

  def getByStatus(status: NonEmptyList[PetStatus]): F[List[Pet]]

  def getByTag(tags: NonEmptyList[String]): F[List[Pet]]

  def list(pageSize: Int, offset: Int): F[List[Pet]]
}
