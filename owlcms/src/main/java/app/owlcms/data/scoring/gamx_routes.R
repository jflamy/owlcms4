# R/gamx_routes.R

library(jsonlite)
library(gamlss.dist)

# ---- 0. Load parameter tables from JSON ----
# Expected structure:
# [
#   { "bodyMass": 40.0, "mu": ..., "sigma": ..., "nu": ... },
#   ...
# ]

# Determine absolute base directory of the API
base_dir <- dirname(normalizePath("../plumber.R"))

# Construct absolute JSON file paths
men_json_path <- file.path(base_dir, "data", "params_sen_men.json")
wom_json_path <- file.path(base_dir, "data", "params_sen_wom.json")

# Load JSON parameter tables
params_men <- jsonlite::fromJSON(men_json_path)
params_wom <- jsonlite::fromJSON(wom_json_path)

# Ensure numeric columns
params_men$bodyMass <- as.numeric(params_men$bodyMass)
params_wom$bodyMass <- as.numeric(params_wom$bodyMass)

# ---- Helper: core GAMX computation from total + mu,sigma,nu ----
compute_gamx_core <- function(total_num, mu_num, sigma_num, nu_num) {
  # Range checks: 0 cannot be used
  if (total_num <= 0 || mu_num <= 0 || sigma_num <= 0) {
    return(list(
      success       = FALSE,
      error_code    = "INVALID_PARAMETER_RANGE",
      error_message = "Parameters total, mu and sigma must be greater than 0.",
      received      = list(
        total  = total_num,
        mu     = mu_num,
        sigma  = sigma_num,
        nu     = nu_num
      )
    ))
  }

  # Compute p using BCCG
  p <- tryCatch(
    gamlss.dist::pBCCG(total_num, mu_num, sigma_num, nu_num),
    error = function(e) NA
  )

  if (is.na(p) || is.nan(p) || is.infinite(p)) {
    return(list(
      success       = FALSE,
      error_code    = "BCCG_COMPUTE_ERROR",
      error_message = "Failed to compute p-value from BCCG distribution.",
      details       = list(
        total  = total_num,
        mu     = mu_num,
        sigma  = sigma_num,
        nu     = nu_num
      )
    ))
  }

  # Transform p to z-score
  z <- tryCatch(
    qnorm(p),
    error = function(e) NA
  )

  if (is.na(z) || is.nan(z) || is.infinite(z)) {
    return(list(
      success       = FALSE,
      error_code    = "NORM_COMPUTE_ERROR",
      error_message = "Failed to transform p-value to z-score (qnorm).",
      p_value       = p
    ))
  }

  gamx         <- z * 100 + 1000
  gamx_rounded <- round(gamx, 2)

  list(
    success       = TRUE,
    total         = total_num,
    mu            = mu_num,
    sigma         = sigma_num,
    nu            = nu_num,
    p             = p,
    z             = z,
    gamx          = gamx,
    gamx_rounded = sprintf("%.2f", gamx_rounded)
  )
}

# ---- Helper: interpolate mu/sigma/nu from body_mass + sex ----
interpolate_params <- function(sex_char, body_mass_num) {
  sex_upper <- toupper(sex_char)

  if (!(sex_upper %in% c("M", "W"))) {
    return(list(
      success       = FALSE,
      error_code    = "INVALID_SEX",
      error_message = "Parameter 'sex' must be 'M' or 'W'.",
      received      = list(sex = sex_char)
    ))
  }

  df <- if (sex_upper == "M") params_men else params_wom

  # body_mass range check
  min_bm <- min(df$bodyMass, na.rm = TRUE)
  max_bm <- max(df$bodyMass, na.rm = TRUE)

  if (body_mass_num < min_bm || body_mass_num > max_bm) {
    return(list(
      success       = FALSE,
      error_code    = "BODY_MASS_OUT_OF_RANGE",
      error_message = sprintf(
        "body_mass is out of supported range [%.2f, %.2f].",
        min_bm, max_bm
      ),
      received      = list(body_mass = body_mass_num, sex = sex_upper)
    ))
  }

  # indices of closest lower / higher bodyMass
  low_idx  <- max(which(df$bodyMass <= body_mass_num))
  high_idx <- min(which(df$bodyMass >= body_mass_num))

  # safety: should not happen if range is checked
  if (length(low_idx) == 0 || length(high_idx) == 0 ||
      is.infinite(low_idx) || is.infinite(high_idx)) {
    return(list(
      success       = FALSE,
      error_code    = "BODY_MASS_MATCH_ERROR",
      error_message = "Failed to find matching rows for body_mass.",
      received      = list(body_mass = body_mass_num, sex = sex_upper)
    ))
  }

  low_bm  <- df$bodyMass[low_idx]
  high_bm <- df$bodyMass[high_idx]

  if (low_idx == high_idx || abs(high_bm - low_bm) < .Machine$double.eps) {
    # Exact match or numerically identical
    mu    <- df$mu[low_idx]
    sigma <- df$sigma[low_idx]
    nu    <- df$nu[low_idx]
  } else {
    # Linear interpolation, Excel-like:
    # low_ratio  = body_mass - low_bm
    # high_ratio = high_bm   - body_mass
    # param = (high_ratio * low + low_ratio * high) / (low_ratio + high_ratio)
    low_ratio  <- body_mass_num - low_bm
    high_ratio <- high_bm - body_mass_num
    denom      <- low_ratio + high_ratio

    mu    <- (high_ratio * df$mu[low_idx]    + low_ratio * df$mu[high_idx])    / denom
    sigma <- (high_ratio * df$sigma[low_idx] + low_ratio * df$sigma[high_idx]) / denom
    nu    <- (high_ratio * df$nu[low_idx]    + low_ratio * df$nu[high_idx])    / denom
  }

  list(
    success      = TRUE,
    sex          = sex_upper,
    body_mass    = body_mass_num,
    body_mass_lo = low_bm,
    body_mass_hi = high_bm,
    mu           = mu,
    sigma        = sigma,
    nu           = nu
  )
}

# ---- Health check endpoint ----
#' Health check endpoint
#' @get /health
function() {
  list(
    status  = "ok",
    service = "gamx-api",
    time    = as.character(Sys.time())
  )
}

# ---- Version info ----
#' Version info endpoint
#' @get /version
function() {
  list(
    service  = "gamx-api",
    version  = "1.1.0",
    language = "R",
    details  = list(
      formula = "GAMX = qnorm(pBCCG(total, mu, sigma, nu)) * 100 + 1000",
      modes   = c("direct_mu_sigma_nu" = "/gamx",
                  "from_body_mass"     = "/gamx_full")
    )
  )
}

# ---- Original endpoint: /gamx (direct params) ----
#' Calculate GAMX score from explicit mu, sigma, nu parameters
#'
#' @param total Total lifted kg
#' @param mu    Mu parameter of the BCCG distribution
#' @param sigma Sigma parameter of the BCCG distribution
#' @param nu    Nu parameter of the BCCG distribution
#'
#' @get /gamx
function(total, mu, sigma, nu) {

  if (is.null(total) || is.null(mu) || is.null(sigma) || is.null(nu)) {
    return(list(
      success       = FALSE,
      error_code    = "MISSING_PARAMETERS",
      error_message = "Missing required query parameters: total, mu, sigma, nu.",
      example       = "/gamx?total=150&mu=350&sigma=0.08&nu=4.4"
    ))
  }

  total_num <- suppressWarnings(as.numeric(total))
  mu_num    <- suppressWarnings(as.numeric(mu))
  sigma_num <- suppressWarnings(as.numeric(sigma))
  nu_num    <- suppressWarnings(as.numeric(nu))

  if (any(is.na(c(total_num, mu_num, sigma_num, nu_num)))) {
    return(list(
      success       = FALSE,
      error_code    = "INVALID_PARAMETER_FORMAT",
      error_message = "Parameters total, mu, sigma, nu must be numeric.",
      received      = list(
        total  = total,
        mu     = mu,
        sigma  = sigma,
        nu     = nu
      )
    ))
  }

  compute_gamx_core(total_num, mu_num, sigma_num, nu_num)
}

# ---- NEW endpoint: /gamx_full (sex + body_mass + total) ----
#' Calculate GAMX score from sex, body_mass and total.
#'
#' The API internally looks up and interpolates (mu, sigma, nu)
#' from JSON parameter tables for men and women.
#'
#' @param sex        Sex of the athlete ("M" or "W")
#' @param body_mass  Body mass in kg
#' @param total      Total lifted kg
#'
#' @get /gamx_full
function(sex, body_mass, total) {

  # Presence check
  if (is.null(sex) || is.null(body_mass) || is.null(total)) {
    return(list(
      success       = FALSE,
      error_code    = "MISSING_PARAMETERS",
      error_message = "Missing required query parameters: sex, body_mass, total.",
      example       = "/gamx_full?sex=M&body_mass=111.11&total=150"
    ))
  }

  body_mass_num <- suppressWarnings(as.numeric(body_mass))
  total_num     <- suppressWarnings(as.numeric(total))

  if (any(is.na(c(body_mass_num, total_num)))) {
    return(list(
      success       = FALSE,
      error_code    = "INVALID_PARAMETER_FORMAT",
      error_message = "Parameters body_mass and total must be numeric.",
      received      = list(
        sex        = sex,
        body_mass  = body_mass,
        total      = total
      )
    ))
  }

  # Interpolate mu, sigma, nu from JSON tables
  interp <- interpolate_params(sex, body_mass_num)

  if (isFALSE(interp$success)) {
    # Interpolation failed → propagate the error
    return(interp)
  }

  # Compute GAMX from interpolated parameters
  res <- compute_gamx_core(
    total_num  = total_num,
    mu_num     = interp$mu,
    sigma_num  = interp$sigma,
    nu_num     = interp$nu
  )

  # If core computation failed, propagate but keep context
  if (isFALSE(res$success)) {
    res$interpolation <- interp
    return(res)
  }

  # Enrich successful result with interpolation info
  res$sex          <- interp$sex
  res$body_mass    <- interp$body_mass
  res$body_mass_lo <- interp$body_mass_lo
  res$body_mass_hi <- interp$body_mass_hi

  res
}