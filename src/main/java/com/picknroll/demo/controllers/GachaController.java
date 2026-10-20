package com.picknroll.demo.controllers;

import com.picknroll.demo.interceptors.JwtInterceptor;
import com.picknroll.demo.models.gacha.*;
import com.picknroll.demo.services.GachaServices;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/activity/gacha")
@CrossOrigin(origins = {"https://1290520169185280062.discordsays.com", "https://1308185877419130891.discordsays.com"})
public class GachaController {

    static final String FORBIDDEN_BODY = "{\"error\":\"Forbidden\"}";
    static final String INSUFFICIENT_ROLLS_BODY = "{\"error\":\"Insufficient Rolls\"}";
    static final String INVALID_COUNT_BODY = "{\"error\":\"count must be 1 or 10\"}";
    static final String EMPTY_POOL_BODY = "{\"error\":\"No eligible players for pull\"}";

    private final GachaServices gachaServices;

    public GachaController(GachaServices gachaServices) {
        this.gachaServices = gachaServices;
    }

    @GetMapping("/wallet")
    public ResponseEntity<?> wallet(HttpServletRequest request) {
        String userId = currentUserId(request);
        if (userId == null) {
            return new ResponseEntity<>(FORBIDDEN_BODY, HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(gachaServices.getWallet(userId));
    }

    @GetMapping("/inventory")
    public ResponseEntity<?> inventory(HttpServletRequest request) {
        String userId = currentUserId(request);
        if (userId == null) {
            return new ResponseEntity<>(FORBIDDEN_BODY, HttpStatus.FORBIDDEN);
        }
        List<GachaInventoryItem> items = gachaServices.getInventory(userId);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/rates")
    public GachaRatesResponse rates() {
        return gachaServices.getRates();
    }

    @PostMapping("/pull")
    public ResponseEntity<?> pull(@RequestBody GachaPullRequest body, HttpServletRequest request) {
        String userId = currentUserId(request);
        if (userId == null) {
            return new ResponseEntity<>(FORBIDDEN_BODY, HttpStatus.FORBIDDEN);
        }
        int count = body == null ? 0 : body.getCount();
        GachaServices.PullOutcome outcome = gachaServices.pull(userId, count);
        if (outcome.success()) {
            return ResponseEntity.ok(outcome.response());
        }
        return switch (outcome.error()) {
            case GachaServices.INSUFFICIENT_ROLLS ->
                    new ResponseEntity<>(INSUFFICIENT_ROLLS_BODY, HttpStatus.BAD_REQUEST);
            case GachaServices.INVALID_COUNT ->
                    new ResponseEntity<>(INVALID_COUNT_BODY, HttpStatus.BAD_REQUEST);
            case GachaServices.EMPTY_POOL ->
                    new ResponseEntity<>(EMPTY_POOL_BODY, HttpStatus.BAD_REQUEST);
            default -> new ResponseEntity<>("{\"error\":\"Pull failed\"}", HttpStatus.BAD_REQUEST);
        };
    }

    private String currentUserId(HttpServletRequest request) {
        Object attribute = request.getAttribute(JwtInterceptor.USER_DATA_ATTRIBUTE);
        if (!(attribute instanceof Claims claims)) {
            return null;
        }
        Object id = claims.get("id");
        return id == null ? null : id.toString();
    }
}
