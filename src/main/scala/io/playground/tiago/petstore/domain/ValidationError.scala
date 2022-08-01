package io.playground.tiago.petstore.domain

import users.User
import pets.Pet

sealed trait ValidationError extends Product with Serializable

// User
case object UserNotFoundError extends ValidationError

case class UserAlreadyExistsError(user: User) extends ValidationError

case class UserAuthenticationFailedError(userName: String)
    extends ValidationError

// Pet
case object PetNotFoundError extends ValidationError

case class PetAlreadyExistsError(pet: Pet) extends ValidationError
