"""
JWT validation matching Spring Boot JwtUtil.java:
  - Secret: Base64-decoded HMAC key (Decoders.BASE64.decode(secret))
  - Algorithm: HS256
  - Claims: sub=email, userId, role
"""
import base64
import os
from dataclasses import dataclass

import jwt
from jwt import ExpiredSignatureError, InvalidTokenError


@dataclass
class UserContext:
    user_id: str
    email: str
    role: str  # "ADMIN" | "CORPORATE" | "INDIVIDUAL"


class JwtValidator:
    def __init__(self, jwt_secret: str | None = None):
        raw = jwt_secret or os.environ["JWT_SECRET"]
        # Match JwtUtil.java: Decoders.BASE64.decode(secret)
        self._key = base64.b64decode(raw)

    def decode(self, token: str) -> UserContext:
        """Decode and validate a JWT access token. Raises on invalid/expired."""
        try:
            payload = jwt.decode(token, self._key, algorithms=["HS256"])
        except ExpiredSignatureError:
            raise ValueError("Token has expired")
        except InvalidTokenError as e:
            raise ValueError(f"Invalid token: {e}")

        # Refresh tokens have type=refresh — reject them
        if payload.get("type") == "refresh":
            raise ValueError("Refresh token cannot be used for API access")

        return UserContext(
            user_id=payload["userId"],
            email=payload["sub"],
            role=payload["role"],
        )


# Module-level singleton
_validator: JwtValidator | None = None


def get_validator() -> JwtValidator:
    global _validator
    if _validator is None:
        _validator = JwtValidator()
    return _validator


def decode_token(token: str) -> UserContext:
    return get_validator().decode(token)
