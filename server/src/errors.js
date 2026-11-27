export class ApiError extends Error {
  constructor(status, message, details) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details;
  }
}

export function asyncHandler(handler) {
  return (req, res, next) => Promise.resolve(handler(req, res, next)).catch(next);
}

export function badRequest(message, details) {
  return new ApiError(400, message, details);
}

export function notFound(message = "Not found") {
  return new ApiError(404, message);
}
