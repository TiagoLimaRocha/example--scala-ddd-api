package io.playground.tiago.petstore.infrastructure

import io.playground.tiago.petstore.domain.users.User

import org.http4s.Response
import tsec.authentication.{AugmentedJWT, SecuredRequest, TSecAuthService}

package object endpoint {
  type AuthService[F[_], Auth] =
    TSecAuthService[User, AugmentedJWT[Auth, Long], F]

  type AuthEndpoint[F[_], Auth] =
    PartialFunction[SecuredRequest[F, User, AugmentedJWT[Auth, Long]], F[
      Response[F]
    ]]
}
