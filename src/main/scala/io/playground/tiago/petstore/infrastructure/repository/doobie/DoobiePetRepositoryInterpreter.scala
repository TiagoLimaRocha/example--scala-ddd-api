package io.playground.tiago.petstore.infrastructure.repository.doobie

import io.playground.tiago.petstore.domain.pets.{
  Pet,
  PetRepositoryAlgebra,
  PetStatus
}

import cats.data._
import cats.syntax.all._
import cats.effect.Bracket

import doobie.implicits._
import doobie._

import SQLPagination._

private object PetSQL {
  /* We require type StatusMeta to handle our ADT Status */
  implicit val StatusMeta: Meta[PetStatus] =
    Meta[String].imap(PetStatus.withName)(_.entryName)

  /* This is used to marshal our sets of strings */
  implicit val SetStringMeta: Meta[Set[String]] =
    Meta[String].imap(_.split(',').toSet)(_.mkString(","))

  def insert(pet: Pet): Update0 = sql""" 
    INSERT INTO PET (
      NAME,
      CATEGORY,
      BIO,
      STATUS,
      TAGS,
      PHOTO_URLS
    ) VALUES (
      ${pet.name},
      ${pet.category},
      ${pet.bio},
      ${pet.status},
      ${pet.tags},
      ${pet.photoUrls}
    )
  """.update

  def update(pet: Pet, id: Long): Update0 = sql"""
    UPDATE PET SET 
      NAME = ${pet.name},
      BIO = ${pet.bio}, 
      STATUS = ${pet.status},
      TAGS = ${pet.tags},
      PHOTO_URLS = ${pet.photoUrls}
    WHERE ID = $id
  """.update

  def delete(id: Long): Update0 = sql"""
    DELETE FROM PET WHERE ID = $id
  """.update

  def select(id: Long): Query0[Pet] = sql"""
    SELECT * FROM PET WHERE ID = $id
  """.query

  def selectByNameAndCategory(name: String, category: String): Query0[Pet] =
    sql"""
    SELECT * FROM PET WHERE NAME = $name AND CATEGORY = $category
  """.query

  def selectByStatus(status: NonEmptyList[PetStatus]): Query0[Pet] = (sql"""
    SELECT * FROM PET WHERE STATUS = 
  """ ++ Fragments.in(fr"STATUS", status)).query

  def selectByTag(tags: NonEmptyList[String]): Query0[Pet] = {
    val tagLikeString: String =
      tags.toList.mkString("TAGS LIKE '%", "%' OR TAGS LIKE '%", "%'")

    (sql"""SELECT * FROM PET WHERE""" ++ Fragment.const(tagLikeString))
      .query[Pet]
  }

  def selectAll: Query0[Pet] = sql"""
   SELECT * FROM PET ORDER BY NAME
  """.query[Pet]
}

class DoobiePetRepositoryInterpreter[F[_]: Bracket[*[_], Throwable]](
    val xa: Transactor[F]
) extends PetRepositoryAlgebra[F] {
  def create(pet: Pet): F[Pet] =
    PetSQL
      .insert(pet)
      .withUniqueGeneratedKeys[Long]("ID")
      .map(id => pet.copy(id = id.some))
      .transact(xa)

  def update(pet: Pet): F[Option[Pet]] =
    OptionT
      .fromOption[ConnectionIO](pet.id)
      .semiflatMap(id => PetSQL.update(pet, id).run.as(pet))
      .value
      .transact(xa)

  def delete(id: Long): F[Option[Pet]] =
    OptionT(PetSQL.select(id).option)
      .semiflatMap(pet => PetSQL.delete(id).run.as(pet))
      .value
      .transact(xa)

  def get(id: Long): F[Option[Pet]] =
    PetSQL
      .select(id)
      .option
      .transact(xa)

  def getByNameAndCategory(name: String, category: String): F[Set[Pet]] =
    PetSQL
      .selectByNameAndCategory(name, category)
      .to[List]
      .transact(xa)
      .map(_.toSet)

  def getByStatus(status: NonEmptyList[PetStatus]): F[List[Pet]] =
    PetSQL
      .selectByStatus(status)
      .to[List]
      .transact(xa)

  def getByTag(tags: NonEmptyList[String]): F[List[Pet]] =
    PetSQL
      .selectByTag(tags)
      .to[List]
      .transact(xa)

  def list(pageSize: Int, offset: Int): F[List[Pet]] =
    paginate(pageSize, offset)(PetSQL.selectAll)
      .to[List]
      .transact(xa)
}

object DoobiePetRepositoryInterpreter {
  def apply[F[_]: Bracket[*[_], Throwable]](
      xa: Transactor[F]
  ): DoobiePetRepositoryInterpreter[F] =
    new DoobiePetRepositoryInterpreter(xa)
}
