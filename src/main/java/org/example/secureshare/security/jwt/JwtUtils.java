package org.example.secureshare.security.jwt;

import org.example.secureshare.security.services.UserDetailsImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;


@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${spring.app.jwtExpirationMs}")
    private int jwtExpirationsMs;
    @Value("${spring.app.jwtSecret}")
    private String jwtSecret;

    @Value("${spring.ecom.app.jwtCookieName}")
    private String jwtCookie;

    //Getting JWT Token from Header
    public String getJwtFromHeader(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            logger.debug("Authorization Header: {}", header);
            return null;
        }
        return header.substring(7);
    }

    public String getJwtFromCookie(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, jwtCookie);
        if (cookie != null) {
            logger.debug("JWT Cookie found: {}", cookie.getValue());
            return cookie.getValue();
        } else {
            logger.debug("JWT Cookie not found");
            return null;
        }
    }

    public String generateTokenFromUserName(String username){
        return Jwts.builder().subject(username)
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime()+jwtExpirationsMs))
                .signWith(key())
                .compact();
    }

    //Generating token from cookie
    public ResponseCookie generateTokenFromCookie(UserDetailsImpl userPrincipal) {
        String jwt = generateTokenFromUserName(userPrincipal.getUsername());
        return ResponseCookie.from(jwtCookie, jwt)
                .httpOnly(true)
                .path("/api")
                .maxAge(24*60*60) // 24 hours
                .build();
    }

    //Generating no Token for cookie for Signout
    public ResponseCookie generateNoTokenFromCookie() {
        return ResponseCookie.from(jwtCookie, null)
                .path("/api")
                .build();
    }

    //Getting username from a token
    public String getUsernameFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith((SecretKey) key())
                    .build().parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (Exception e) {
            logger.error("Cannot parse JWT token: {}", token, e);
            return null;
        }
    }

    //Generating key for signing
    public Key key() {
        return Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(jwtSecret));
    }

    //Validating authToken
    public boolean validateToken(String authToken) {
        try{
            System.out.println("Validating");
            Jwts.parser()
                    .verifyWith((SecretKey) key())
                    .build().parseSignedClaims(authToken);
            return true;
        }
        catch (MalformedJwtException exception){
            logger.error("Invalid JWT token: {}", exception.getMessage());
        }
        catch(ExpiredJwtException exception){
            logger.error("JWT token is expired: {}", exception.getMessage());
        }
        catch (UnsupportedJwtException exception){
            logger.error("JWT token is unsupported: {}", exception.getMessage());
        }
        catch (IllegalArgumentException exception){
            logger.error("JWT claims string is empty: {}", exception.getMessage());
        }
        return false;
    }


}
