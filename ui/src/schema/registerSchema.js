import * as yup from "yup";

const nameRegex = /^[\p{L}]+([\p{L} '\-]*[\p{L}])?$/u;

export const registerSchema = yup.object({
  firstName: yup
    .string()
    .trim()
    .required("First name is required")
    .max(50, "First name must be max 50 characters")
    .matches(nameRegex, "First name can contain only letters"),

  lastName: yup
    .string()
    .trim()
    .required("Last name is required")
    .max(50, "Last name must be max 50 characters")
    .matches(nameRegex, "Last name can contain only letters"),

  username: yup
    .string()
    .trim()
    .required("Username is required")
    .min(3, "Username must be at least 3 characters")
    .max(50, "Username must be max 50 characters"),

  email: yup
    .string()
    .trim()
    .required("Email is required")
    .email("Email must be a valid email address"),

  password: yup
    .string()
    .required("Password is required")
    .min(8, "Password must be at least 8 characters")
    .max(100, "Password must be max 100 characters"),
});
