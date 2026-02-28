import * as yup from "yup";

export const  schema = yup.object({
  instrument: yup.string().trim().required("Instrument is required"),
  side: yup
    .mixed()
    .oneOf(["BUY", "SELL"], "Side must be BUY or SELL")
    .required("Side is required"),
  price: yup
    .number()
    .typeError("Price must be a number")
    .moreThan(0, "Price must be positive")
    .required("Price is required"),
  quantity: yup
    .number()
    .typeError("Quantity must be a number")
    .integer("Quantity must be an integer")
    .moreThan(0, "Quantity must be positive")
    .required("Quantity is required"),
});