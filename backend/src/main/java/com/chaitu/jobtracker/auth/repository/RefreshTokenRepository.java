package com.chaitu.jobtracker.auth.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chaitu.jobtracker.auth.entity.RefreshToken;

/** Slice 2b adds finders here (by token hash, by user). Today only save() is used. */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
}
