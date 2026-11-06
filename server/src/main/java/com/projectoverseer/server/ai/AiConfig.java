package com.projectoverseer.server.ai;

public record AiConfig(
    float maxHealth,
    float wanderSpeed,
    float chaseSpeed,
    float fleeSpeed,
    float wanderRadius,
    float aggroRadius,
    float leashRadius,
    float attackRange,
    float attackDamage,
    float attackCooldown,
    float fleeHealthThreshold,
    float safeDistance
) {}
