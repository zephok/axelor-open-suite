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
package com.axelor.apps.businessproject.service;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.apps.account.service.AccountManagementAccountService;
import com.axelor.apps.account.service.AccountingSituationService;
import com.axelor.apps.account.service.analytic.AnalyticMoveLineService;
import com.axelor.apps.account.service.app.AppAccountService;
import com.axelor.apps.account.service.move.MoveCreateService;
import com.axelor.apps.account.service.move.MoveValidateService;
import com.axelor.apps.account.service.moveline.MoveLineConsolidateService;
import com.axelor.apps.account.service.moveline.MoveLineCreateService;
import com.axelor.apps.account.service.payment.PaymentModeService;
import com.axelor.apps.hr.db.ExpenseLine;
import com.axelor.apps.hr.db.repo.ExpenseRepository;
import com.axelor.apps.hr.service.config.AccountConfigHRService;
import com.axelor.apps.hr.service.config.HRConfigService;
import com.axelor.apps.hr.service.expense.ExpenseServiceImpl;
import com.axelor.apps.message.service.TemplateMessageService;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class ExpenseServiceProjectImpl extends ExpenseServiceImpl {

  @Inject
  public ExpenseServiceProjectImpl(
      MoveCreateService moveCreateService,
      MoveValidateService moveValidateService,
      ExpenseRepository expenseRepository,
      MoveLineCreateService moveLineCreateService,
      AccountManagementAccountService accountManagementAccountService,
      AppAccountService appAccountService,
      AccountConfigHRService accountConfigService,
      AccountingSituationService accountingSituationService,
      AnalyticMoveLineService analyticMoveLineService,
      HRConfigService hrConfigService,
      TemplateMessageService templateMessageService,
      PaymentModeService paymentModeService,
      MoveLineConsolidateService moveLineConsolidateService) {
    super(
        moveCreateService,
        moveValidateService,
        expenseRepository,
        moveLineCreateService,
        accountManagementAccountService,
        appAccountService,
        accountConfigService,
        accountingSituationService,
        analyticMoveLineService,
        hrConfigService,
        templateMessageService,
        paymentModeService,
        moveLineConsolidateService);
  }

  @Override
  public List<InvoiceLine> createInvoiceLines(
      Invoice invoice, List<ExpenseLine> expenseLineList, int priority) throws AxelorException {

    if (!appAccountService.isApp("business-project")) {
      return super.createInvoiceLines(invoice, expenseLineList, priority);
    }

    List<InvoiceLine> invoiceLineList = new ArrayList<InvoiceLine>();
    int count = 0;
    for (ExpenseLine expenseLine : expenseLineList) {

      invoiceLineList.addAll(this.createInvoiceLine(invoice, expenseLine, priority * 100 + count));
      count++;
      invoiceLineList.get(invoiceLineList.size() - 1).setProject(expenseLine.getProject());
    }

    return invoiceLineList;
  }
}
