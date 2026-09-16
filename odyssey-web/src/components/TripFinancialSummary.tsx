import { useState, type FormEvent } from "react";
import { Check, CreditCard, LoaderCircle, Pencil, X } from "lucide-react";
import type { PaymentStatus } from "../types/payment";

export interface ProviderOfferAmount {
  providerPrice: number;
  currency: string;
}

interface TripFinancialSummaryProps {
  assistanceFee?: number;
  providerOffers?: ProviderOfferAmount[];
  editable?: boolean;
  onSaveAssistanceFee?: (assistanceFee: number) => Promise<void>;
  showTravelerExplanation?: boolean;
  paymentStatus?: PaymentStatus | null;
  showPaymentStatus?: boolean;
  checkoutLoading?: boolean;
  checkoutError?: string | null;
  onCheckout?: () => void;
}

function formatPrice(price: number, currency: string) {
  return new Intl.NumberFormat("fr-FR", {
    style: "currency",
    currency,
    maximumFractionDigits: 2,
  }).format(price);
}

function normalizeCurrency(currency: unknown): string | null {
  if (typeof currency !== "string") return null;

  const normalizedCurrency = currency.trim().toUpperCase();
  if (!normalizedCurrency) return null;

  try {
    new Intl.NumberFormat("fr-FR", { style: "currency", currency: normalizedCurrency });
    return normalizedCurrency;
  } catch {
    return null;
  }
}

function getProviderTotals(providerOffers: ProviderOfferAmount[]) {
  return providerOffers.reduce<Map<string, number>>((totals, offer) => {
    const currency = normalizeCurrency(offer.currency);
    if (!currency || !Number.isFinite(offer.providerPrice) || offer.providerPrice < 0) return totals;

    totals.set(currency, (totals.get(currency) ?? 0) + offer.providerPrice);
    return totals;
  }, new Map());
}

export function TripFinancialSummary({
  assistanceFee,
  providerOffers,
  editable = false,
  onSaveAssistanceFee,
  showTravelerExplanation = false,
  paymentStatus,
  showPaymentStatus = false,
  checkoutLoading = false,
  checkoutError,
  onCheckout,
}: TripFinancialSummaryProps) {
  const [editing, setEditing] = useState(false);
  const [feeInput, setFeeInput] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const providerTotals = providerOffers ? getProviderTotals(providerOffers) : null;
  const providerCurrencies = providerTotals ? [...providerTotals.keys()] : [];
  const canCalculateEstimatedTotal =
    assistanceFee !== undefined &&
    providerTotals !== null &&
    (providerCurrencies.length === 0 || (providerCurrencies.length === 1 && providerCurrencies[0] === "EUR"));
  const estimatedTotal = canCalculateEstimatedTotal && providerTotals ? (providerTotals.get("EUR") ?? 0) + assistanceFee : null;

  function startEditing() {
    setFeeInput(assistanceFee === undefined ? "" : String(assistanceFee));
    setError(null);
    setSaved(false);
    setEditing(true);
  }

  function cancelEditing() {
    setEditing(false);
    setError(null);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!onSaveAssistanceFee || saving) return;

    const parsedFee = Number(feeInput);
    if (feeInput.trim() === "" || !Number.isFinite(parsedFee) || parsedFee < 0) {
      setError("Saisissez des frais d’assistance valides.");
      return;
    }

    setSaving(true);
    setError(null);
    setSaved(false);

    try {
      await onSaveAssistanceFee(parsedFee);
      setEditing(false);
      setSaved(true);
    } catch {
      setError("Les frais d’assistance n’ont pas pu être enregistrés.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="trip-financial-summary" aria-label="Synthèse financière du voyage">
      {providerTotals && (
        <div className="trip-financial-row">
          <div>
            <strong>Total des offres fournisseurs</strong>
            <span>Somme des offres retenues disponibles</span>
          </div>
          <div className="trip-financial-values">
            {providerCurrencies.length === 0 ? (
              <strong>Aucune offre retenue</strong>
            ) : (
              providerCurrencies.map((currency) => <strong key={currency}>{formatPrice(providerTotals.get(currency) ?? 0, currency)}</strong>)
            )}
          </div>
        </div>
      )}

      <div className="trip-financial-row trip-assistance-row">
        <div>
          <strong>Frais d’assistance Odyssey</strong>
          <span>Pour l’ensemble du voyage</span>
        </div>
        {editing ? (
          <form className="trip-fee-inline-form" onSubmit={handleSubmit}>
            <label>
              <span className="sr-only">Frais d’assistance en euros</span>
              <input
                type="number"
                min="0"
                step="0.01"
                value={feeInput}
                onChange={(event) => {
                  setFeeInput(event.target.value);
                  setError(null);
                }}
                autoFocus
                required
              />
              <span>€</span>
            </label>
            <button type="submit" className="primary-button" disabled={saving}>
              {saving ? <LoaderCircle className="rotating" size={16} /> : <Check size={16} />}
              {saving ? "Enregistrement…" : "Enregistrer"}
            </button>
            <button type="button" className="secondary-button" disabled={saving} onClick={cancelEditing}>
              <X size={16} /> Annuler
            </button>
          </form>
        ) : (
          <div className="trip-fee-display">
            <strong>{assistanceFee === undefined ? "Non disponible" : formatPrice(assistanceFee, "EUR")}</strong>
            {editable && onSaveAssistanceFee && (
              <button type="button" className="secondary-button" onClick={startEditing}>
                <Pencil size={15} /> Modifier
              </button>
            )}
          </div>
        )}
      </div>

      {error && (
        <p className="trip-financial-error" role="alert">
          {error}
        </p>
      )}
      {saved && (
        <p className="trip-financial-success" role="status">
          <Check size={16} /> Frais d’assistance enregistrés.
        </p>
      )}

      {providerTotals && (
        <div className="trip-financial-row trip-estimated-total">
          <div>
            <strong>Coût total estimé</strong>
            <span>Offres fournisseurs et frais Odyssey</span>
          </div>
          <strong>{estimatedTotal === null ? "Non calculé en présence de devises différentes" : formatPrice(estimatedTotal, "EUR")}</strong>
        </div>
      )}

      {showPaymentStatus && (
        <div className="trip-payment-state">
          {paymentStatus === "PAID" ? (
            <p className="trip-financial-success">
              <Check size={16} /> Frais d’assistance Odyssey payés
            </p>
          ) : (
            <>
              <p>
                {paymentStatus === "PENDING"
                  ? "Paiement des frais d’assistance en attente de confirmation."
                  : paymentStatus === "FAILED"
                    ? "Le paiement précédent a échoué."
                    : "Frais d’assistance Odyssey à régler."}
              </p>
              {onCheckout && assistanceFee !== undefined && assistanceFee > 0 && (
                <button type="button" className="primary-button" disabled={checkoutLoading} onClick={onCheckout}>
                  {checkoutLoading ? <LoaderCircle className="rotating" size={17} /> : <CreditCard size={17} />}
                  {checkoutLoading
                    ? "Redirection…"
                    : `${paymentStatus === "PENDING" ? "Reprendre le paiement" : "Payer les frais d’assistance"} — ${formatPrice(assistanceFee, "EUR")}`}
                </button>
              )}
            </>
          )}
          {checkoutError && (
            <p className="trip-financial-error" role="alert">
              {checkoutError}
            </p>
          )}
        </div>
      )}

      {showTravelerExplanation && (
        <p className="trip-financial-explanation">
          Les prestations de voyage sont réglées directement auprès des fournisseurs. Les frais d’assistance Odyssey correspondent à l’accompagnement
          pour l’ensemble de votre voyage.
        </p>
      )}
    </section>
  );
}
