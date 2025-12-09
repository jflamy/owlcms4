# GAMX2 Specification

## Overview

GAMX2 is a Java implementation of the GAMX scoring system that computes scores using the Box-Cox Cole and Green (BCCG) distribution.

## Formula

```
GAMX = qnorm(pBCCG(total, μ, σ, ν)) × 100 + 1000
```

Where:
- `total` = lifted weight in kg
- `μ`, `σ`, `ν` = BCCG distribution parameters (interpolated from body mass)
- `pBCCG()` = **truncated/normalized** CDF (cumulative distribution function) of the Box-Cox Cole and Green (BCCG) distribution
- `qnorm()` = inverse of the standard normal CDF (probit function)

### pBCCG Detail (from R gamlss.dist)

The BCCG CDF is not a simple Box-Cox transformation. It uses truncation and normalization:

```
z = ((y/μ)^ν - 1) / (ν × σ)     for ν ≠ 0
z = log(y/μ) / σ                 for ν → 0

FYy1 = Φ(z)
FYy2 = Φ(-1/(σ×|ν|))   if ν > 0, else 0
FYy3 = Φ(1/(σ×|ν|))

p = (FYy1 - FYy2) / FYy3
```

The final score is scaled: z-score × 100 + 1000

## Parameter Tables

Four sets of CSV parameter files are loaded from `owlcms/src/main/resources/gamx/`:

| Variant | Men File | Women File | Usage |
|---------|----------|------------|-------|
| SENIOR | `params_sen_men.csv` | `params_sen_wom.csv` | Standard GAMX for seniors |
| AGE_ADJUSTED | `params_iwf_men.csv` | `params_iwf_wom.csv` | GAMX-A for age-adjusted IWF data (ages 13-40) |
| U17 | `params_usa_men.csv` | `params_usa_wom.csv` | GAMX-U for U17 athletes |
| MASTERS | `params_mas_men.csv` | `params_mas_wom.csv` | GAMX-M for masters athletes |

### CSV Format

**Body-mass-only variants (SENIOR, U17):**
```csv
bodyMass,mu,sigma,nu
40.0,123.45,0.085,4.2
41.0,125.67,0.084,4.1
...
```

**Age-dependent variants (AGE_ADJUSTED, MASTERS):**
```csv
age,bodyMass,mu,sigma,nu
30,40.0,115.17,0.0835,2.57
30,40.1,115.48,0.0835,2.56
...
58,69.0,163.77,0.1055,0.53
...
95,190.0,54.42,0.2197,-2.88
```

**Column definitions:**
- `age`: Age in years (only for age-dependent variants, sorted ascending)
- `bodyMass`: Body mass in kg (sorted ascending within each age)
- `mu`: Location parameter (μ) of BCCG distribution
- `sigma`: Scale parameter (σ) of BCCG distribution  
- `nu`: Skewness parameter (ν) of BCCG distribution

**Age ranges by variant:**
- MASTERS: ages 30-95
- AGE_ADJUSTED: ages 13-40

## Algorithms

### 1. Parameter Interpolation

#### Body-Mass-Only Variants (SENIOR, U17)

Given `sex` ("M" or "W") and `bodyMass`:

1. Select appropriate parameter table based on sex
2. **Clamp** body mass to table range `[min_bm, max_bm]`
3. Find bracketing rows:
   - `low_idx` = largest index where `table.bodyMass <= bodyMass`
   - `high_idx` = smallest index where `table.bodyMass >= bodyMass`
4. If exact match (`low_idx == high_idx`), use values directly
5. Otherwise, linear interpolation:

```java
double lowRatio = bodyMass - lowBm;
double highRatio = highBm - bodyMass;
double denom = lowRatio + highRatio;

double mu = (highRatio * lowMu + lowRatio * highMu) / denom;
double sigma = (highRatio * lowSigma + lowRatio * highSigma) / denom;
double nu = (highRatio * lowNu + lowRatio * highNu) / denom;
```

#### Age-Dependent Variants (AGE_ADJUSTED, MASTERS)

Given `sex`, `bodyMass`, and `age`:

1. Select appropriate parameter table based on sex
2. **Clamp age to table bounds** (critical for correct scoring):
   - MASTERS: clamp to `[30, 95]` (e.g., age 25 → 30)
   - AGE_ADJUSTED: clamp to `[13, 40]`
3. **Binary search** to find first row matching `normalizedAge`
4. **Expand** to find all rows with same age (using `normalizedAge`, not original `age`)
5. Extract body-mass rows for that age, then interpolate by body mass as above

```java
// CRITICAL: Use normalizedAge (clamped) not original age when expanding rows
int lastAgeIdx = firstAgeIdx;
while (lastAgeIdx + 1 < params.size() && 
       Math.abs(params.get(lastAgeIdx + 1).age - normalizedAge) < 0.01) {
    lastAgeIdx++;
}
```

**Why age clamping matters:** A 29-year-old allowed to compete as MASTERS should use age 30 parameters (the youngest in the table). Without clamping, the algorithm would fail to find matching rows and return incorrect results.

### 2. BCCG CDF (pBCCG)

The BCCG (Box-Cox Cole and Green) distribution uses a **truncated and normalized** form of the Box-Cox transformation. This matches R's `gamlss.dist::pBCCG` function exactly.

#### Box-Cox Transformation to z-score

```java
double z;
if (Math.abs(nu) < 1e-10) {
    // Limiting case when nu → 0: log-normal distribution
    z = Math.log(total / mu) / sigma;
} else {
    // General case: Box-Cox power transformation
    z = (Math.pow(total / mu, nu) - 1.0) / (nu * sigma);
}
```

#### Truncation and Normalization

The BCCG distribution is **truncated** to positive values and **normalized** to ensure the CDF integrates to 1. This is the key insight from R's implementation:

```java
// Standard normal CDF of the transformed value
double FYy1 = normalCDF(z);

// Lower truncation bound (ensures y > 0)
double FYy2;
if (nu > 0) {
    FYy2 = normalCDF(-1.0 / (sigma * Math.abs(nu)));
} else {
    FYy2 = 0.0;
}

// Upper truncation bound (normalization factor)
double FYy3 = normalCDF(1.0 / (sigma * Math.abs(nu)));

// Normalized CDF: shift by lower bound and scale by valid range
double p = (FYy1 - FYy2) / FYy3;
```

#### Mathematical Interpretation

- **FYy1** = Φ(z) is the standard normal CDF at the transformed point
- **FYy2** = Φ(-1/(σ|ν|)) is the probability mass below y=0 (truncated away when ν > 0)
- **FYy3** = Φ(1/(σ|ν|)) is the normalization factor ensuring CDF reaches 1

The formula `p = (FYy1 - FYy2) / FYy3` shifts and rescales the CDF to account for truncation.

**Note:** Without this truncation/normalization step, computed p-values will be significantly different (e.g., 0.0612 vs 0.000154), leading to GAMX scores off by ~200 points.

### 3. Inverse Normal CDF (qnorm/probit)

Transform probability `p` to z-score using the inverse of the standard normal CDF.

Use Apache Commons Math `NormalDistribution.inverseCumulativeProbability(p)` or equivalent.

### 4. Final GAMX Score

```java
double gamx = z * 100 + 1000;
```

### 5. kgTarget Algorithm (Inverse Problem)

Given a target GAMX score, find the minimum total (in kg) that **strictly exceeds** that score at 2 decimal precision.

#### Purpose

When two athletes have identical age, gender, and bodyweight, and one achieves GAMX score X, the other needs `kgTarget(X)` to guarantee a **win** (not just a tie at 2 decimal precision).

#### Algorithm Steps

1. **Convert target GAMX to probability**:
   ```java
   double z = (targetGAMX - 1000.0) / 100.0;
   double p = normalCDF(z);  // Φ(z)
   ```

2. **Compute initial estimate using qBCCG** (inverse of pBCCG from R gamlss.dist):
   ```java
   double totalEstimate = qBCCG(p, μ, σ, ν);
   ```

3. **Find minimum integer kg using decrement test**:
   ```java
   // Start with ceiling of estimate (likely exceeds target)
   int candidate = (int) Math.ceil(totalEstimate);
   double targetRounded = Math.round(targetGAMX * 100.0) / 100.0;
   
   // If candidate doesn't exceed, increment until it does
   while (candidate < 600) { // impossibly high total value
       double gamxAtCandidate = computeGamx(bodyMass, candidate);
       double gamxRounded = Math.round(gamxAtCandidate * 100.0) / 100.0;
       if (gamxRounded > targetRounded) break;
       candidate++;
   }
   
   // Decrement to find minimum that still exceeds
   while (candidate > 1) {
       int test = candidate - 1;
       double gamxAtTest = computeGamx(bodyMass, test);
       double gamxRounded = Math.round(gamxAtTest * 100.0) / 100.0;
       if (gamxRounded > targetRounded) {
           candidate = test;  // Still exceeds, keep going lower
       } else {
           break;  // test doesn't exceed, candidate is minimum
       }
   }
   return candidate;
   ```

#### qBCCG Implementation (from R gamlss.dist)

Inverse CDF of the BCCG distribution:

```java
/**
 * Compute qBCCG - inverse CDF of BCCG distribution.
 * Decompiled from R gamlss.dist::qBCCG source code.
 */
private static double qBCCG(double p, double mu, double sigma, double nu) {
    if (p <= 0.0 || p >= 1.0) return Double.NaN;
    
    // Adjust probability for truncation (inverse of pBCCG normalization)
    double pAdjusted;
    if (nu <= 0) {
        pAdjusted = p * normalCDF(1.0 / (sigma * Math.abs(nu)));
    } else {
        pAdjusted = 1.0 - (1.0 - p) * normalCDF(1.0 / (sigma * Math.abs(nu)));
    }
    
    // Convert adjusted probability to z-score
    double z = inverseNormalCDF(pAdjusted);  // qnorm
    
    // Inverse Box-Cox transformation
    double total;
    if (Math.abs(nu) < 1e-10) {
        // Limiting case: log-normal
        total = mu * Math.exp(sigma * z);
    } else {
        // General case: power transformation
        total = mu * Math.pow(nu * sigma * z + 1.0, 1.0 / nu);
    }
    
    return total;
}
```

**Key insight from R source**: The probability adjustment step mirrors the truncation/normalization in pBCCG but in reverse - this is critical for correctness.

## Key Implementation Insights from R Source

The R `gamlss.dist` package provided the authoritative implementation:

1. **pBCCG truncation/normalization**: The formula `p = (Φ(z) - FYy2) / FYy3` with truncation bounds was extracted from R source code. This was the critical missing piece - without it, scores were off by ~200 points.

2. **qBCCG probability adjustment**: The inverse function adjusts probability differently based on sign of ν:
   - `ν ≤ 0`: `p' = p × Φ(1/(σ|ν|))`
   - `ν > 0`: `p' = 1 - (1-p) × Φ(1/(σ|ν|))`

3. **Box-Cox limiting case**: When `|ν| < 1e-10`, use log-normal transformation instead of power transformation to avoid numerical instability.

## Validation Rules

### Input Validation

1. `total > 0` (lifted weight must be positive)
2. `mu > 0` (location parameter must be positive)
3. `sigma > 0` (scale parameter must be positive)
4. `bodyMass` within table range

### Output Validation

1. `p` must be in `(0, 1)` exclusive - reject NaN/Infinity
2. `z` must be finite - reject NaN/Infinity
3. Return 0.0 for any invalid computation

## Error Handling

| Condition | Behavior |
|-----------|----------|
| Null gender | Return 0.0 |
| Null/zero body mass | Return 0.0 |
| Null/zero total | Return 0.0 |
| Body mass out of range | Clamp to nearest valid value |
| pBCCG computation error | Return 0.0, log warning |
| qnorm computation error | Return 0.0, log warning |

## Testing

### Reference Test Cases (Men)

| bodyMass | total | Expected GAMX |
|----------|-------|---------------|
| 55.0 | 200 | 827.08 |
| 61.0 | 250 | 892.88 |
| 67.0 | 280 | 916.85 |
| 73.0 | 310 | 961.73 |
| 81.0 | 340 | 1020.74 |
| 89.0 | 370 | 1094.89 |
| 96.0 | 390 | 1134.51 |
| 102.0 | 410 | 1183.90 |
| 109.0 | 430 | 1230.90 |
| 120.0 | 450 | 1300.98 |

### Reference Test Cases (Women)

| bodyMass | total | Expected GAMX |
|----------|-------|---------------|
| 45.0 | 130 | 842.70 |
| 49.0 | 160 | 929.82 |
| 55.0 | 180 | 951.52 |
| 59.0 | 195 | 979.08 |
| 64.0 | 210 | 1008.47 |
| 71.0 | 230 | 1048.06 |
| 76.0 | 245 | 1083.84 |
| 81.0 | 255 | 1112.91 |
| 87.0 | 270 | 1190.52 |
| 100.0 | 290 | 1263.83 |

### Test Coverage

1. ✅ Known input/output pairs matching R reference (tolerance < 0.01)
2. ✅ Interpolation at exact body mass values
3. ✅ Interpolation between body mass values  
4. ✅ Edge cases: minimum/maximum body mass (clamping)
5. ✅ Error handling for invalid inputs (null, zero, negative)
6. ⬜ All four variants (SENIOR, AGE_ADJUSTED, U17, MASTERS) - pending parameter files
7. ✅ Both genders

