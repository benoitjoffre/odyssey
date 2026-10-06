import { cloneElement, isValidElement, useId, type ComponentPropsWithoutRef, type ReactElement, type ReactNode } from "react";
import "./FormField.css";

type FormControlProps = {
  id?: string;
  name?: string;
  "aria-invalid"?: boolean;
  "aria-describedby"?: string;
};

export type FormFieldProps = Omit<ComponentPropsWithoutRef<"div">, "children"> & {
  label: ReactNode;
  required?: boolean;
  helperText?: ReactNode;
  error?: ReactNode;
  children: ReactElement<FormControlProps>;
};

function mergeDescribedBy(...values: Array<string | undefined>) {
  return values.filter(Boolean).join(" ") || undefined;
}

export function FormField({ label, required = false, helperText, error, children, className, id, ...fieldProps }: FormFieldProps) {
  const generatedId = useId();
  const controlId = id ?? generatedId;
  const helperId = helperText ? `${controlId}-help` : undefined;
  const errorId = error ? `${controlId}-error` : undefined;
  const describedBy = mergeDescribedBy(helperId, errorId);

  const fieldChildren = isValidElement<FormControlProps>(children)
    ? cloneElement(children, {
        id: children.props.id ?? controlId,
        name: children.props.name ?? controlId,
        "aria-invalid": Boolean(error) || children.props["aria-invalid"] || undefined,
        "aria-describedby": mergeDescribedBy(children.props["aria-describedby"], describedBy),
      })
    : children;

  return (
    <div {...fieldProps} className={["ui-form-field", error ? "ui-form-field--error" : "", className].filter(Boolean).join(" ")}>
      <label htmlFor={controlId} className="ui-form-field__label">
        <span>{label}</span>
        {required ? (
          <span className="ui-form-field__required" aria-hidden="true">
            *
          </span>
        ) : null}
      </label>

      {fieldChildren}

      {helperText ? (
        <p id={helperId} className="ui-form-field__help">
          {helperText}
        </p>
      ) : null}

      {error ? (
        <p id={errorId} className="ui-form-field__error" role="alert">
          {error}
        </p>
      ) : null}
    </div>
  );
}
