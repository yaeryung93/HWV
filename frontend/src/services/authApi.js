import { requestApi } from "./api";

export async function loginUser(email, password) {
  return requestApi("/users/login", {
    method: "POST",
    body: JSON.stringify({
      email,
      password,
    }),
  });
}

export async function signupUser(userData) {
  return requestApi("/users/register", {
    method: "POST",
    body: JSON.stringify(userData),
  });
}