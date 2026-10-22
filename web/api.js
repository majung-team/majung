export class ApiError extends Error {
  constructor(status, body) {
    super(
      body?.message || "요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요.",
    );
    this.status = status;
    this.code = body?.code;
  }
}

export async function request(path, { method = "GET", token, body } = {}) {
  const headers = { Accept: "application/json" };
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers["Content-Type"] = "application/json";
  const response = await fetch(`/api/v1/pickups${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    credentials: "omit",
    cache: "no-store",
    redirect: "error",
    referrerPolicy: "no-referrer",
    signal: AbortSignal.timeout(8000),
  });
  const data = await response.json();
  if (!response.ok) throw new ApiError(response.status, data);
  return data;
}
