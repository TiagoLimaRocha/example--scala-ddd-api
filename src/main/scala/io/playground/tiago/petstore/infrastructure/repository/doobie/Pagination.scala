package io.playground.tiago.petstore.infrastructure.repository.doobie

import doobie._
import doobie.implicits._

trait SQLPagination {
  def limit[A: Read](lim: Int)(q: Query0[A]): Query0[A] =
    (q.toFragment ++ fr"LIMIT $lim").query

  def paginate[A: Read](lim: Int, offset: Int)(q: Query0[A]): Query0[A] =
    (q.toFragment ++ fr"LIMIT $lim OFFSET $offset").query
}

object SQLPagination extends SQLPagination
