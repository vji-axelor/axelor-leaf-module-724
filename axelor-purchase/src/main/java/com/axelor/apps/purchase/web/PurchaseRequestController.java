/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.apps.purchase.web;

import com.axelor.apps.base.AxelorException;
import com.axelor.apps.base.db.repo.TraceBackRepository;
import com.axelor.apps.base.service.exception.TraceBackService;
import com.axelor.apps.purchase.db.PurchaseOrder;
import com.axelor.apps.purchase.db.PurchaseRequest;
import com.axelor.apps.purchase.db.PurchaseRequestLine;
import com.axelor.apps.purchase.db.PurchaseRequestValidator;
import com.axelor.apps.purchase.db.PurchaseRequestValidatorGroup;
import com.axelor.apps.purchase.db.repo.PurchaseRequestRepository;
import com.axelor.apps.purchase.db.repo.PurchaseRequestValidatorGroupRepository;
import com.axelor.apps.purchase.exception.PurchaseExceptionMessage;
import com.axelor.apps.purchase.service.PurchaseRequestService;
import com.axelor.apps.purchase.service.PurchaseRequestWorkflowService;
import com.axelor.auth.db.User;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.meta.schema.actions.ActionView.ActionViewBuilder;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.utils.StringTool;
import com.google.inject.Singleton;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class PurchaseRequestController {

  public void generatePo(ActionRequest request, ActionResponse response) {
    @SuppressWarnings("unchecked")
    List<Long> requestIds = (List<Long>) request.getContext().get("_ids");
    if (requestIds != null && !requestIds.isEmpty()) {
      Boolean groupBySupplier = (Boolean) request.getContext().get("groupBySupplier");
      groupBySupplier = groupBySupplier == null ? false : groupBySupplier;
      Boolean groupByProduct = (Boolean) request.getContext().get("groupByProduct");
      groupByProduct = groupByProduct == null ? false : groupByProduct;
      try {
        List<PurchaseRequest> purchaseRequests =
            Beans.get(PurchaseRequestRepository.class)
                .all()
                .filter("self.id in (?1)", requestIds)
                .fetch();
        List<String> purchaseRequestSeqs =
            purchaseRequests.stream()
                .filter(pr -> pr.getSupplierUser() == null)
                .map(PurchaseRequest::getPurchaseRequestSeq)
                .collect(Collectors.toList());
        if (purchaseRequestSeqs != null && !purchaseRequestSeqs.isEmpty()) {
          throw new AxelorException(
              TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
              I18n.get(PurchaseExceptionMessage.PURCHASE_REQUEST_MISSING_SUPPLIER_USER),
              purchaseRequestSeqs.toString());
        }
        response.setCanClose(true);
        List<PurchaseOrder> purchaseOrderList =
            Beans.get(PurchaseRequestService.class)
                .generatePo(purchaseRequests, groupBySupplier, groupByProduct);
        ActionViewBuilder actionViewBuilder =
            ActionView.define(
                    String.format(
                        "Purchase Order%s generated", (purchaseOrderList.size() > 1 ? "s" : "")))
                .model(PurchaseOrder.class.getName())
                .add("grid", "purchase-order-quotation-grid")
                .add("form", "purchase-order-form")
                .param("search-filters", "purchase-order-filters")
                .context("_showSingle", true)
                .domain(
                    String.format(
                        "self.id in (%s)", StringTool.getIdListString(purchaseOrderList)));
        response.setView(actionViewBuilder.map());
      } catch (AxelorException e) {
        response.setInfo(e.getMessage());
      }
    }
  }

  public void requestPurchaseRequest(ActionRequest request, ActionResponse response) {
    try {
      PurchaseRequest purchaseRequest = request.getContext().asType(PurchaseRequest.class);
      purchaseRequest = Beans.get(PurchaseRequestRepository.class).find(purchaseRequest.getId());
      Beans.get(PurchaseRequestWorkflowService.class).requestPurchaseRequest(purchaseRequest);
      response.setReload(true);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void acceptPurchaseRequest(ActionRequest request, ActionResponse response) {
    try {
      PurchaseRequest purchaseRequest = request.getContext().asType(PurchaseRequest.class);
      purchaseRequest = Beans.get(PurchaseRequestRepository.class).find(purchaseRequest.getId());
      Beans.get(PurchaseRequestWorkflowService.class).acceptPurchaseRequest(purchaseRequest);
      response.setReload(true);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void purchasePurchaseRequest(ActionRequest request, ActionResponse response) {
    try {
      PurchaseRequest purchaseRequest = request.getContext().asType(PurchaseRequest.class);
      purchaseRequest = Beans.get(PurchaseRequestRepository.class).find(purchaseRequest.getId());
      Beans.get(PurchaseRequestWorkflowService.class).purchasePurchaseRequest(purchaseRequest);
      response.setReload(true);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void refusePurchaseRequest(ActionRequest request, ActionResponse response) {
    try {
      PurchaseRequest purchaseRequest = request.getContext().asType(PurchaseRequest.class);
      purchaseRequest = Beans.get(PurchaseRequestRepository.class).find(purchaseRequest.getId());
      Beans.get(PurchaseRequestWorkflowService.class).refusePurchaseRequest(purchaseRequest);
      response.setReload(true);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void cancelPurchaseRequest(ActionRequest request, ActionResponse response) {
    try {
      PurchaseRequest purchaseRequest = request.getContext().asType(PurchaseRequest.class);
      purchaseRequest = Beans.get(PurchaseRequestRepository.class).find(purchaseRequest.getId());
      Beans.get(PurchaseRequestWorkflowService.class).cancelPurchaseRequest(purchaseRequest);
      response.setReload(true);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void draftPurchaseRequest(ActionRequest request, ActionResponse response) {
    try {
      PurchaseRequest purchaseRequest = request.getContext().asType(PurchaseRequest.class);
      purchaseRequest = Beans.get(PurchaseRequestRepository.class).find(purchaseRequest.getId());
      Beans.get(PurchaseRequestWorkflowService.class).draftPurchaseRequest(purchaseRequest);
      response.setReload(true);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void validateRequestAction(ActionRequest request, ActionResponse response) {
    PurchaseRequest purchaseRequest = request.getContext().asType(PurchaseRequest.class);
    List<PurchaseRequestValidator> PurchaseRequestValidatorList =
        purchaseRequest.getValidatorUserList();
    if (PurchaseRequestValidatorList.size() == 0) {
      response.setError("Please Add validator.");
    }
  }

  public void validateAcceptAction(ActionRequest request, ActionResponse response) {
    PurchaseRequest purchaseRequest = request.getContext().asType(PurchaseRequest.class);
    List<PurchaseRequestValidator> purchaseRequestValidatorList =
        purchaseRequest.getValidatorUserList();

    for (PurchaseRequestValidator PurchaseRequestValidator : purchaseRequestValidatorList) {
      if (!PurchaseRequestValidator.getIsApproved()) {
        response.setError("Please approve request from all validator.");
      }
    }
  }

  public void calculateToalAmount(ActionRequest request, ActionResponse response) {
    PurchaseRequest purchaseRequest = request.getContext().asType(PurchaseRequest.class);
    BigDecimal totalAmount = new BigDecimal(0);
    for (PurchaseRequestLine purchaseRequestLine : purchaseRequest.getPurchaseRequestLineList()) {
      totalAmount =
          totalAmount.add(
              purchaseRequestLine
                  .getQuantity()
                  .multiply(purchaseRequestLine.getProduct().getPurchasePrice()));
    }

    response.setValue("totalAmount", totalAmount);
  }

  public void setRequestValidator(ActionRequest request, ActionResponse response) {

    PurchaseRequest purchaseRequest = request.getContext().asType(PurchaseRequest.class);

    List<PurchaseRequestValidatorGroup> purchaseRequestValidatorGroupMinList =
        Beans.get(PurchaseRequestValidatorGroupRepository.class)
            .all()
            .filter("self.totalAmount >= ?", purchaseRequest.getTotalAmount())
            .fetch();
    PurchaseRequestValidatorGroup selectedValidatorGroup = null;
    if (purchaseRequestValidatorGroupMinList.size() > 0) {
      // get minumum
      PurchaseRequestValidatorGroup minGroup = purchaseRequestValidatorGroupMinList.get(0);

      for (PurchaseRequestValidatorGroup group : purchaseRequestValidatorGroupMinList) {
        if (group.getTotalAmount().compareTo(minGroup.getTotalAmount()) < 0) {
          minGroup = group;
        }
      }
      selectedValidatorGroup = minGroup;
    } else {
      List<PurchaseRequestValidatorGroup> purchaseRequestValidatorGroupMaxList =
          Beans.get(PurchaseRequestValidatorGroupRepository.class)
              .all()
              .filter("self.totalAmount <= ?", purchaseRequest.getTotalAmount())
              .fetch();

      if (purchaseRequestValidatorGroupMaxList.size() == 0) {
        response.setError("Please Create purchase request validator group.");
      }
      // get maximum
      PurchaseRequestValidatorGroup maxGroup = purchaseRequestValidatorGroupMinList.get(0);
      for (PurchaseRequestValidatorGroup group : purchaseRequestValidatorGroupMaxList) {
        if (group.getTotalAmount().compareTo(maxGroup.getTotalAmount()) > 0) {
          maxGroup = group;
        }
      }
      selectedValidatorGroup = maxGroup;
    }

    if (selectedValidatorGroup != null) {
      Set<User> userList = selectedValidatorGroup.getValidatorUser();
      List<PurchaseRequestValidator> purchaseRequestValidatorList =
          new ArrayList<PurchaseRequestValidator>();
      for (User user : userList) {
        PurchaseRequestValidator purchaseRequestValidator = new PurchaseRequestValidator();
        purchaseRequestValidator.setValidatorUser(user);
        purchaseRequestValidatorList.add(purchaseRequestValidator);
      }
      response.setValue("validatorUserList", purchaseRequestValidatorList);
    }
  }
}
