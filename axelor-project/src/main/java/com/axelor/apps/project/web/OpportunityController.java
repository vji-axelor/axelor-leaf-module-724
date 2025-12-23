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
package com.axelor.apps.project.web;

import com.axelor.apps.base.service.exception.TraceBackService;
import com.axelor.apps.crm.db.Opportunity;
import com.axelor.apps.crm.db.repo.OpportunityRepository;
import com.axelor.apps.crm.service.app.AppCrmService;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.project.db.repo.ProjectRepository;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class OpportunityController {

  @Transactional(rollbackOn = {Exception.class})
  @SuppressWarnings("unchecked")
  public void closeWonOpportunity(ActionRequest request, ActionResponse response) {
    try {
      Context context = request.getContext();
      Opportunity opportunity = context.asType(Opportunity.class);
      opportunity = Beans.get(OpportunityRepository.class).find(opportunity.getId());

      Project projectOpportunity = (Project) context.get("projectOpportunity");
      if (projectOpportunity != null) {
        projectOpportunity = Beans.get(ProjectRepository.class).find(projectOpportunity.getId());
        opportunity.setProjectOpportunity(projectOpportunity);
      } else {
        opportunity.setProjectOpportunity(null);
      }

      opportunity.setOpportunityStatus(
          Beans.get(AppCrmService.class).getClosedWinOpportunityStatus());
      Beans.get(OpportunityRepository.class).save(opportunity);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }
}
