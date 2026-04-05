"""
Unit tests for JWT validation.
Tokens are minted using the same Base64-HMAC-SHA256 scheme as Spring Boot JwtUtil.java.
"""
import base64
import time

import jwt
import pytest

# Set env before importing validator
import os
os.environ.setdefault("JWT_SECRET", "ZGV2LXNlY3JldC1rZXktMzJjaGFycy1taW5pbXVtLXJlcXVpcmVk")

from auth.jwt_validator import JwtValidator, decode_token

_SECRET = "ZGV2LXNlY3JldC1rZXktMzJjaGFycy1taW5pbXVtLXJlcXVpcmVk"
_KEY = base64.b64decode(_SECRET)


def _mint_token(payload: dict, exp_offset: int = 3600) -> str:
    payload = {**payload, "exp": int(time.time()) + exp_offset}
    return jwt.encode(payload, _KEY, algorithm="HS256")


def test_valid_admin_token():
    token = _mint_token({"sub": "admin@test.com", "userId": "abc123", "role": "ADMIN"})
    validator = JwtValidator(_SECRET)
    ctx = validator.decode(token)
    assert ctx.email == "admin@test.com"
    assert ctx.user_id == "abc123"
    assert ctx.role == "ADMIN"


def test_valid_individual_token():
    token = _mint_token({"sub": "user@test.com", "userId": "uid999", "role": "INDIVIDUAL"})
    ctx = decode_token(token)
    assert ctx.role == "INDIVIDUAL"


def test_expired_token_raises():
    token = _mint_token({"sub": "x@x.com", "userId": "x", "role": "ADMIN"}, exp_offset=-1)
    with pytest.raises(ValueError, match="expired"):
        decode_token(token)


def test_refresh_token_rejected():
    token = _mint_token({"sub": "x@x.com", "userId": "x", "role": "ADMIN", "type": "refresh"})
    with pytest.raises(ValueError, match="Refresh token"):
        decode_token(token)


def test_tampered_token_raises():
    token = _mint_token({"sub": "x@x.com", "userId": "x", "role": "ADMIN"})
    tampered = token[:-5] + "XXXXX"
    with pytest.raises(ValueError):
        decode_token(tampered)
