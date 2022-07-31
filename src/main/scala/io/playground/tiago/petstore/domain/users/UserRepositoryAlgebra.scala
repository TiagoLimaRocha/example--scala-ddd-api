package io.playground.tiago.petstore.domain.users

import cats.data.OptionT

trait UserRepositoryAlgebra[F[_]] {
  def create(user: User): F[User]

  def update(user: User): OptionT[F, User]

  def delete(userId: Long): OptionT[F, User]

  def deleteByName(userName: String): OptionT[F, User]

  def get(userId: Long): OptionT[F, User]
  
  def getByName(userName: String): OptionT[F, User]

  def list(pageSize: Int, offset: Int): F[List[User]]
}