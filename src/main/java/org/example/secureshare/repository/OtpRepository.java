package org.example.secureshare.repository;

import org.example.secureshare.model.Otp;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpRepository extends JpaRepository<Otp,String> {
}
