export function parseApiError(err, fallback = "Something went wrong") {
  const data = err?.response?.data;

  const message = data?.message || fallback;
  const fieldErrors = Array.isArray(data?.fieldErrors) ? data.fieldErrors : [];

  return { message, fieldErrors };
}
