/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2022 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.account.web;

import com.axelor.apps.account.db.AccountingSituation;
import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.apps.account.db.InvoicePayment;
import com.axelor.apps.account.db.PaymentCondition;
import com.axelor.apps.account.db.PaymentMode;
import com.axelor.apps.account.db.repo.InvoiceRepository;
import com.axelor.apps.account.exception.IExceptionMessage;
import com.axelor.apps.account.service.AccountingSituationService;
import com.axelor.apps.account.service.IrrecoverableService;
import com.axelor.apps.account.service.app.AppAccountService;
import com.axelor.apps.account.service.invoice.InvoiceLineService;
import com.axelor.apps.account.service.invoice.InvoiceService;
import com.axelor.apps.account.service.invoice.InvoiceToolService;
import com.axelor.apps.account.service.invoice.print.InvoicePrintService;
import com.axelor.apps.account.service.payment.invoice.payment.InvoicePaymentCreateService;
import com.axelor.apps.base.db.BankDetails;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.db.PrintingSettings;
import com.axelor.apps.base.db.repo.LanguageRepository;
import com.axelor.apps.base.db.repo.PartnerRepository;
import com.axelor.apps.base.service.AddressService;
import com.axelor.apps.base.service.BankDetailsService;
import com.axelor.apps.base.service.PartnerPriceListService;
import com.axelor.apps.base.service.PartnerService;
import com.axelor.apps.base.service.TradingNameService;
import com.axelor.apps.tool.StringTool;
import com.axelor.common.ObjectUtils;
import com.axelor.exception.AxelorException;
import com.axelor.exception.ResponseMessageType;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.exception.service.TraceBackService;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.google.common.base.Function;
import com.google.inject.Singleton;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InvoiceController {

  @SuppressWarnings("unused")
  private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Fonction appeler par le bouton calculer
   *
   * @param request
   * @param response
   * @return
   */
  public void compute(ActionRequest request, ActionResponse response) {

    Invoice invoice = request.getContext().asType(Invoice.class);

    try {
      invoice = Beans.get(InvoiceService.class).compute(invoice);
      response.setValues(invoice);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  /**
   * Fonction appeler par le bouton valider
   *
   * @param request
   * @param response
   * @return
   */
  public void validate(ActionRequest request, ActionResponse response) throws AxelorException {

    Invoice invoice = request.getContext().asType(Invoice.class);
    invoice = Beans.get(InvoiceRepository.class).find(invoice.getId());

    try {
      // we have to inject TraceBackService to use non static methods
      TraceBackService traceBackService = Beans.get(TraceBackService.class);
      long tracebackCount = traceBackService.countMessageTraceBack(invoice);
      Beans.get(InvoiceService.class).validate(invoice);
      response.setReload(true);
      if (traceBackService.countMessageTraceBack(invoice) > tracebackCount) {
        traceBackService
            .findLastMessageTraceBack(invoice)
            .ifPresent(
                traceback ->
                    response.setNotify(
                        String.format(
                            I18n.get(
                                com.axelor.apps.message.exception.IExceptionMessage
                                    .SEND_EMAIL_EXCEPTION),
                            traceback.getMessage())));
      }
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  /**
   * Called from invoice form view, on clicking ventilate button. Call {@link
   * InvoiceService#ventilate(Invoice)}.
   *
   * @param request
   * @param response
   */
  public void ventilate(ActionRequest request, ActionResponse response) {

    Invoice invoice = request.getContext().asType(Invoice.class);
    invoice = Beans.get(InvoiceRepository.class).find(invoice.getId());

    try {
      // we have to inject TraceBackService to use non static methods
      TraceBackService traceBackService = Beans.get(TraceBackService.class);
      long tracebackCount = traceBackService.countMessageTraceBack(invoice);
      Beans.get(InvoiceService.class).ventilate(invoice);
      response.setReload(true);
      if (traceBackService.countMessageTraceBack(invoice) > tracebackCount) {
        traceBackService
            .findLastMessageTraceBack(invoice)
            .ifPresent(
                traceback ->
                    response.setNotify(
                        String.format(
                            I18n.get(
                                com.axelor.apps.message.exception.IExceptionMessage
                                    .SEND_EMAIL_EXCEPTION),
                            traceback.getMessage())));
      }
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  /**
   * Called by the validate button, if the ventilation is skipped in invoice config
   *
   * @param request
   * @param response
   */
  public void validateAndVentilate(ActionRequest request, ActionResponse response) {

    Invoice invoice = request.getContext().asType(Invoice.class);
    invoice = Beans.get(InvoiceRepository.class).find(invoice.getId());

    try {
      // we have to inject TraceBackService to use non static methods
      TraceBackService traceBackService = Beans.get(TraceBackService.class);
      long tracebackCount = traceBackService.countMessageTraceBack(invoice);
      Beans.get(InvoiceService.class).validateAndVentilate(invoice);
      response.setReload(true);
      if (traceBackService.countMessageTraceBack(invoice) > tracebackCount) {
        traceBackService
            .findLastMessageTraceBack(invoice)
            .ifPresent(
                traceback ->
                    response.setNotify(
                        String.format(
                            I18n.get(
                                com.axelor.apps.message.exception.IExceptionMessage
                                    .SEND_EMAIL_EXCEPTION),
                            traceback.getMessage())));
      }
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  /**
   * Passe l'état de la facture à "annulée"
   *
   * @param request
   * @param response
   * @throws AxelorException
   */
  public void cancel(ActionRequest request, ActionResponse response) throws AxelorException {

    Invoice invoice = request.getContext().asType(Invoice.class);
    invoice = Beans.get(InvoiceRepository.class).find(invoice.getId());

    if (invoice.getStatusSelect() == InvoiceRepository.STATUS_VENTILATED
        && invoice.getCompany().getAccountConfig() != null
        && !invoice.getCompany().getAccountConfig().getAllowCancelVentilatedInvoice()) {
      response.setError(
          I18n.get(
              IExceptionMessage
                  .INVOICE_CAN_NOT_GO_BACK_TO_VALIDATE_STATUS_OR_CANCEL_VENTILATED_INVOICE));
      return;
    }

    Beans.get(InvoiceService.class).cancel(invoice);
    response.setFlash(I18n.get(IExceptionMessage.INVOICE_1));
    response.setReload(true);
  }

  /**
   * Function returning both the paymentMode and the paymentCondition
   *
   * @param request
   * @param response
   */
  public void fillPaymentModeAndCondition(ActionRequest request, ActionResponse response) {
    Invoice invoice = request.getContext().asType(Invoice.class);
    try {
      if (invoice.getOperationTypeSelect() == null) {
        return;
      }
      PaymentMode paymentMode = InvoiceToolService.getPaymentMode(invoice);
      PaymentCondition paymentCondition = InvoiceToolService.getPaymentCondition(invoice);
      response.setValue("paymentMode", paymentMode);
      response.setValue("paymentCondition", paymentCondition);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void checkNotImputedRefunds(ActionRequest request, ActionResponse response) {
    Invoice invoice = request.getContext().asType(Invoice.class);
    invoice = Beans.get(InvoiceRepository.class).find(invoice.getId());

    try {
      String msg = Beans.get(InvoiceService.class).checkNotImputedRefunds(invoice);
      if (msg != null) {
        response.setFlash(msg);
      }
    } catch (AxelorException e) {
      TraceBackService.trace(response, e);
    }
  }

  public void checkNotLetteredAdvancePaymentMoveLines(
      ActionRequest request, ActionResponse response) {
    Invoice invoice = request.getContext().asType(Invoice.class);
    invoice = Beans.get(InvoiceRepository.class).find(invoice.getId());

    try {
      String msg = Beans.get(InvoiceService.class).checkNotLetteredAdvancePaymentMoveLines(invoice);
      if (msg != null) {
        response.setFlash(msg);
      }
    } catch (AxelorException e) {
      TraceBackService.trace(response, e);
    }
  }

  /**
   * Fonction appeler par le bouton générer un avoir.
   *
   * @param request
   * @param response
   */
  public void createRefund(ActionRequest request, ActionResponse response) {

    Invoice invoice = request.getContext().asType(Invoice.class);

    try {

      invoice = Beans.get(InvoiceRepository.class).find(invoice.getId());
      Invoice refund = Beans.get(InvoiceService.class).createRefund(invoice);
      response.setReload(true);
      response.setNotify(I18n.get(IExceptionMessage.INVOICE_2));

      response.setView(
          ActionView.define(
                  String.format(I18n.get(IExceptionMessage.INVOICE_4), invoice.getInvoiceId()))
              .model(Invoice.class.getName())
              .add("form", "invoice-form")
              .add("grid", "invoice-grid")
              .param("search-filters", "customer-invoices-filters")
              .param("forceTitle", "true")
              .context("_showRecord", refund.getId().toString())
              .domain("self.originalInvoice.id = " + invoice.getId())
              .map());
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void usherProcess(ActionRequest request, ActionResponse response) {

    Invoice invoice = request.getContext().asType(Invoice.class);
    invoice = Beans.get(InvoiceRepository.class).find(invoice.getId());

    try {
      Beans.get(InvoiceService.class).usherProcess(invoice);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void passInIrrecoverable(ActionRequest request, ActionResponse response) {

    Invoice invoice = request.getContext().asType(Invoice.class);
    invoice = Beans.get(InvoiceRepository.class).find(invoice.getId());

    try {
      Beans.get(IrrecoverableService.class).passInIrrecoverable(invoice, true);
      response.setReload(true);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void notPassInIrrecoverable(ActionRequest request, ActionResponse response) {

    Invoice invoice = request.getContext().asType(Invoice.class);
    invoice = Beans.get(InvoiceRepository.class).find(invoice.getId());

    try {
      Beans.get(IrrecoverableService.class).notPassInIrrecoverable(invoice);
      response.setReload(true);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  /** Method to generate invoice as a Pdf */
  @SuppressWarnings("unchecked")
  public void showInvoice(ActionRequest request, ActionResponse response) {
    Context context = request.getContext();
    String fileLink;
    String title;

    try {
      if (!ObjectUtils.isEmpty(request.getContext().get("_ids"))) {
        List<Long> ids =
            (List)
                (((List) context.get("_ids"))
                    .stream()
                        .filter(ObjectUtils::notEmpty)
                        .map(input -> Long.parseLong(input.toString()))
                        .collect(Collectors.toList()));
        fileLink = Beans.get(InvoicePrintService.class).printInvoices(ids);
        title = I18n.get("Invoices");
      } else if (context.get("id") != null) {
        String format = context.get("format") != null ? context.get("format").toString() : "pdf";
        Integer reportType =
            context.get("reportType") != null
                ? Integer.parseInt(context.get("reportType").toString())
                : null;

        Map languageMap =
            reportType != null
                    && (reportType == 1 || reportType == 3)
                    && context.get("language") != null
                ? (Map<String, Object>) request.getContext().get("language")
                : null;
        String locale =
            languageMap != null && languageMap.get("id") != null
                ? Beans.get(LanguageRepository.class)
                    .find(Long.parseLong(languageMap.get("id").toString()))
                    .getCode()
                : null;

        fileLink =
            Beans.get(InvoicePrintService.class)
                .printInvoice(
                    Beans.get(InvoiceRepository.class)
                        .find(Long.parseLong(context.get("id").toString())),
                    false,
                    format,
                    reportType,
                    locale);
        title = I18n.get("Invoice");
      } else {
        throw new AxelorException(
            TraceBackRepository.CATEGORY_MISSING_FIELD, I18n.get(IExceptionMessage.INVOICE_3));
      }
      response.setView(ActionView.define(title).add("html", fileLink).map());
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void regenerateAndShowInvoice(ActionRequest request, ActionResponse response) {
    Context context = request.getContext();
    Invoice invoice =
        Beans.get(InvoiceRepository.class).find(Long.parseLong(context.get("id").toString()));
    Integer reportType =
        context.get("reportType") != null
            ? Integer.parseInt(context.get("reportType").toString())
            : null;

    try {
      response.setCanClose(true);
      response.setView(
          ActionView.define(I18n.get("Invoice"))
              .add(
                  "html",
                  Beans.get(InvoicePrintService.class)
                      .printInvoice(invoice, true, "pdf", reportType, null))
              .map());
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  private String buildMassMessage(int doneCount, int errorCount) {
    StringBuilder sb = new StringBuilder();
    sb.append(
        String.format(
            I18n.get(
                com.axelor.apps.base.exceptions.IExceptionMessage.ABSTRACT_BATCH_DONE_SINGULAR,
                com.axelor.apps.base.exceptions.IExceptionMessage.ABSTRACT_BATCH_DONE_PLURAL,
                doneCount),
            doneCount));
    sb.append(" ");
    sb.append(
        String.format(
            I18n.get(
                com.axelor.apps.base.exceptions.IExceptionMessage.ABSTRACT_BATCH_ANOMALY_SINGULAR,
                com.axelor.apps.base.exceptions.IExceptionMessage.ABSTRACT_BATCH_ANOMALY_PLURAL,
                errorCount),
            errorCount));
    return sb.toString();
  }

  private void massProcess(
      ActionRequest request,
      ActionResponse response,
      Function<Collection<? extends Number>, Pair<Integer, Integer>> function) {

    try {
      @SuppressWarnings("unchecked")
      List<Number> ids = (List<Number>) request.getContext().get("_ids");

      if (ObjectUtils.isEmpty(ids)) {
        response.setError(com.axelor.apps.base.exceptions.IExceptionMessage.RECORD_NONE_SELECTED);
        return;
      }

      Pair<Integer, Integer> massCount = function.apply(ids);

      String message = buildMassMessage(massCount.getLeft(), massCount.getRight());
      response.setFlash(message);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    } finally {
      response.setReload(true);
    }
  }

  public void massValidation(ActionRequest request, ActionResponse response) {
    try {
      Function<Collection<? extends Number>, Pair<Integer, Integer>> function;

      if (Beans.get(AppAccountService.class).getAppInvoice().getIsVentilationSkipped()) {
        function = Beans.get(InvoiceService.class)::massValidateAndVentilate;
      } else {
        function = Beans.get(InvoiceService.class)::massValidate;
      }

      massProcess(request, response, function);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void massVentilation(ActionRequest request, ActionResponse response) {
    try {
      massProcess(request, response, Beans.get(InvoiceService.class)::massVentilate);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void computeAddressStr(ActionRequest request, ActionResponse response) {
    Invoice invoice = request.getContext().asType(Invoice.class);
    response.setValue(
        "addressStr", Beans.get(AddressService.class).computeAddressStr(invoice.getAddress()));
  }

  public void computeDeliveryAddressStr(ActionRequest request, ActionResponse response) {
    Invoice invoice = request.getContext().asType(Invoice.class);
    response.setValue(
        "deliveryAddressStr",
        Beans.get(AddressService.class).computeAddressStr(invoice.getDeliveryAddress()));
  }

  /**
   * Called on load and in partner, company or payment mode change. Fill the bank details with a
   * default value.
   *
   * @param request
   * @param response
   * @throws AxelorException
   */
  public void fillCompanyBankDetails(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Invoice invoice = request.getContext().asType(Invoice.class);
    PaymentMode paymentMode = invoice.getPaymentMode();
    Company company = invoice.getCompany();
    Partner partner = invoice.getPartner();
    if (company == null) {
      return;
    }
    if (partner != null) {
      partner = Beans.get(PartnerRepository.class).find(partner.getId());
    }
    BankDetails defaultBankDetails =
        Beans.get(BankDetailsService.class)
            .getDefaultCompanyBankDetails(
                company, paymentMode, partner, invoice.getOperationTypeSelect());
    response.setValue("companyBankDetails", defaultBankDetails);
  }

  /**
   * Called on load and on new, create the domain for the field {@link
   * Invoice#advancePaymentInvoiceSet}
   *
   * @param request
   * @param response
   */
  public void fillAdvancePaymentInvoiceSetDomain(ActionRequest request, ActionResponse response) {

    Invoice invoice = request.getContext().asType(Invoice.class);
    try {
      String domain = Beans.get(InvoiceService.class).createAdvancePaymentInvoiceSetDomain(invoice);
      response.setAttr("advancePaymentInvoiceSet", "domain", domain);

    } catch (Exception e) {
      TraceBackService.trace(e);
      response.setError(e.getMessage());
    }
  }

  /**
   * Called on partner and currency change, fill the domain of the field {@link
   * Invoice#advancePaymentInvoiceSet} with default values. The default values are every invoices
   * found in the domain.
   *
   * @param request
   * @param response
   */
  public void fillAdvancePaymentInvoiceSet(ActionRequest request, ActionResponse response) {

    Invoice invoice = request.getContext().asType(Invoice.class);
    try {
      Set<Invoice> invoices =
          Beans.get(InvoiceService.class).getDefaultAdvancePaymentInvoice(invoice);
      response.setValue("advancePaymentInvoiceSet", invoices);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  /**
   * set default value for automatic invoice printing
   *
   * @param request
   * @param response
   * @throws AxelorException
   */
  public void setDefaultMail(ActionRequest request, ActionResponse response) {
    Invoice invoice = request.getContext().asType(Invoice.class);
    Company company = invoice.getCompany();
    Partner partner = invoice.getPartner();
    if (company != null && partner != null) {
      AccountingSituation accountingSituation =
          Beans.get(AccountingSituationService.class).getAccountingSituation(partner, company);
      if (accountingSituation != null) {
        response.setValue("invoiceAutomaticMail", accountingSituation.getInvoiceAutomaticMail());
        response.setValue(
            "invoiceMessageTemplate", accountingSituation.getInvoiceMessageTemplate());
        response.setValue(
            "invoiceAutomaticMailOnValidate",
            accountingSituation.getInvoiceAutomaticMailOnValidate());
        response.setValue(
            "invoiceMessageTemplateOnValidate",
            accountingSituation.getInvoiceMessageTemplateOnValidate());
      }
    }
  }

  /**
   * Called on printing settings select. Set the domain for {@link Invoice#printingSettings}
   *
   * @param request
   * @param response
   */
  public void filterPrintingSettings(ActionRequest request, ActionResponse response) {
    Invoice invoice = request.getContext().asType(Invoice.class);

    List<PrintingSettings> printingSettingsList =
        Beans.get(TradingNameService.class)
            .getPrintingSettingsList(invoice.getTradingName(), invoice.getCompany());
    String domain =
        String.format(
            "self.id IN (%s)",
            !printingSettingsList.isEmpty()
                ? StringTool.getIdListString(printingSettingsList)
                : "0");

    response.setAttr("printingSettings", "domain", domain);
  }

  /**
   * Called on trading name change. Set the default value for {@link Invoice#printingSettings}
   *
   * @param request
   * @param response
   */
  public void fillDefaultPrintingSettings(ActionRequest request, ActionResponse response) {
    try {
      Invoice invoice = request.getContext().asType(Invoice.class);
      response.setValue(
          "printingSettings",
          Beans.get(TradingNameService.class)
              .getDefaultPrintingSettings(invoice.getTradingName(), invoice.getCompany()));
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  /**
   * Called from invoice form view on partner change. Get the default price list for the invoice.
   * Call {@link PartnerPriceListService#getDefaultPriceList(Partner, int)}.
   *
   * @param request
   * @param response
   */
  public void fillPriceList(ActionRequest request, ActionResponse response) {
    try {
      Invoice invoice = request.getContext().asType(Invoice.class);
      Partner partner = invoice.getPartner();
      if (partner == null) {
        return;
      }
      int priceListTypeSelect = Beans.get(InvoiceService.class).getPurchaseTypeOrSaleType(invoice);
      response.setValue(
          "priceList",
          Beans.get(PartnerPriceListService.class)
              .getDefaultPriceList(partner, priceListTypeSelect));
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  /**
   * Called from invoice view on price list select. Call {@link
   * PartnerPriceListService#getPriceListDomain(Partner, int)}.
   *
   * @param request
   * @param response
   */
  public void changePriceListDomain(ActionRequest request, ActionResponse response) {
    try {
      Invoice invoice = request.getContext().asType(Invoice.class);
      int priceListTypeSelect = Beans.get(InvoiceService.class).getPurchaseTypeOrSaleType(invoice);
      String domain =
          Beans.get(PartnerPriceListService.class)
              .getPriceListDomain(invoice.getPartner(), priceListTypeSelect);
      response.setAttr("priceList", "domain", domain);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void massPaymentOnSupplierInvoices(ActionRequest request, ActionResponse response) {
    try {
      Context context = request.getContext();

      if (!ObjectUtils.isEmpty(context.get("_ids"))) {
        List<Long> invoiceIdList =
            (List)
                (((List) context.get("_ids"))
                    .stream()
                        .filter(ObjectUtils::notEmpty)
                        .map(input -> Long.parseLong(input.toString()))
                        .collect(Collectors.toList()));

        List<Long> invoiceToPay =
            Beans.get(InvoicePaymentCreateService.class).getInvoiceIdsToPay(invoiceIdList);

        if (invoiceToPay.isEmpty()) {
          response.setError(I18n.get(IExceptionMessage.INVOICE_NO_INVOICE_TO_PAY));
        }

        response.setView(
            ActionView.define(I18n.get("Register a mass payment"))
                .model(InvoicePayment.class.getName())
                .add("form", "invoice-payment-mass-form")
                .param("popup", "reload")
                .param("show-toolbar", "false")
                .param("show-confirm", "false")
                .param("popup-save", "false")
                .param("forceEdit", "true")
                .context("_invoices", invoiceToPay)
                .map());
      }
    } catch (Exception e) {
      TraceBackService.trace(response, e, ResponseMessageType.ERROR);
    }
  }

  public void checkPartnerBankDetailsList(ActionRequest request, ActionResponse response) {
    Invoice invoice = request.getContext().asType(Invoice.class);
    response.setAttr(
        "$partnerBankDetailsListWarning",
        "hidden",
        Beans.get(InvoiceService.class).checkPartnerBankDetailsList(invoice));
  }

  public void refusalToPay(ActionRequest request, ActionResponse response) {
    Invoice invoice = request.getContext().asType(Invoice.class);
    Beans.get(InvoiceService.class)
        .refusalToPay(
            Beans.get(InvoiceRepository.class).find(invoice.getId()),
            invoice.getReasonOfRefusalToPay(),
            invoice.getReasonOfRefusalToPayStr());
    response.setCanClose(true);
  }

  public void setPfpValidatorUser(ActionRequest request, ActionResponse response) {
    Invoice invoice = request.getContext().asType(Invoice.class);
    response.setValue(
        "pfpValidatorUser", Beans.get(InvoiceService.class).getPfpValidatorUser(invoice));
  }

  public void setPfpValidatorUserDomain(ActionRequest request, ActionResponse response) {
    Invoice invoice = request.getContext().asType(Invoice.class);
    response.setAttr(
        "pfpValidatorUser",
        "domain",
        Beans.get(InvoiceService.class).getPfpValidatorUserDomain(invoice));
  }

  public void hideSendEmailPfpBtn(ActionRequest request, ActionResponse response) {
    Invoice invoice = request.getContext().asType(Invoice.class);
    if (invoice.getPfpValidatorUser() == null) {
      return;
    }
    response.setAttr(
        "$isSelectedPfpValidatorEqualsPartnerPfpValidator",
        "value",
        invoice
            .getPfpValidatorUser()
            .equals(Beans.get(InvoiceService.class).getPfpValidatorUser(invoice)));
  }

  public void getInvoicePartnerDomain(ActionRequest request, ActionResponse response) {
    Invoice invoice = request.getContext().asType(Invoice.class);
    Company company = invoice.getCompany();

    long companyId = company.getPartner() == null ? 0 : company.getPartner().getId();

    String domain = String.format("self.id != %d AND self.isContact = false ", companyId);
    domain += " AND :company member of self.companySet";

    int invoiceTypeSelect = Beans.get(InvoiceService.class).getPurchaseTypeOrSaleType(invoice);

    if (invoiceTypeSelect == 1) {
      domain += " AND self.isCustomer = true ";
    } else {
      domain += " AND self.isSupplier = true ";
    }

    try {

      if ((!(invoice.getInvoiceLineList() == null || invoice.getInvoiceLineList().isEmpty()))
          && (invoiceTypeSelect == 1)) {

        domain += Beans.get(PartnerService.class).getPartnerDomain(invoice.getPartner());
      }

    } catch (Exception e) {
      TraceBackService.trace(e);
      response.setError(e.getMessage());
    }
    response.setAttr("partner", "domain", domain);
  }

  public void showDuplicateInvoiceNbrWarning(ActionRequest request, ActionResponse response) {
    try {
      Invoice invoice = request.getContext().asType(Invoice.class);
      boolean isDuplicateInvoiceNbr =
          Beans.get(InvoiceService.class).getIsDuplicateInvoiceNbr(invoice);
      response.setAttr("$duplicateInvoiceNbr", "hidden", !isDuplicateInvoiceNbr);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  /**
   * Called from invoice form view upon changing the fiscalPosition Updates taxLine, taxEquiv and
   * prices by calling {@link InvoiceLineService#fillProductInformation(Invoice, InvoiceLine)} and
   * {@link InvoiceLineService#compute(Invoice, InvoiceLine)}
   *
   * @param request
   * @param response
   */
  public void updateLinesAfterFiscalPositionChange(ActionRequest request, ActionResponse response) {
    try {
      Invoice invoice = request.getContext().asType(Invoice.class);
      if (invoice.getInvoiceLineList() != null) {
        InvoiceLineService invoiceLineService = Beans.get(InvoiceLineService.class);
        for (InvoiceLine invoiceLine : invoice.getInvoiceLineList()) {
          invoiceLineService.updateLinesAfterFiscalPositionChange(invoice);
          response.setValue("invoiceLineList", invoice.getInvoiceLineList());
        }
      }
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }
}
