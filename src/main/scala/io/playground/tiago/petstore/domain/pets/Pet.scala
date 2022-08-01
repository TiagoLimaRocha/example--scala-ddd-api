package io.playground.tiago.petstore.domain.pets

case class Pet(
  id: Option[Long] = None,
  name: String,
  category: String,
  bio: String,
  status: PetStatus = PetStatus.Available,
  photoUrls: Set[String] = Set.empty,
  tags: Set[String] = Set.empty
)