/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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
package com.axelor.apps.account.service;

import java.math.BigDecimal;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.apps.account.db.BankStatement;
import com.axelor.apps.account.db.BankStatementLine;
import com.axelor.apps.account.db.Move;
import com.axelor.apps.account.db.MoveLine;
import com.axelor.apps.account.exception.IExceptionMessage;
import com.axelor.apps.account.service.administration.GeneralServiceAccount;
import com.axelor.apps.base.db.Partner;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.IException;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class BankStatementService {
	
	private static final Logger LOG = LoggerFactory.getLogger(BankStatementService.class);
	
	@Inject
	private MoveService moveService;
	
	@Inject
	private MoveLineService moveLineService;
	
	
	
	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public void compute(BankStatement bankStatement) throws AxelorException  {
		
		BigDecimal computedBalance = bankStatement.getStartingBalance();
		
		for(BankStatementLine bankStatementLine : bankStatement.getBankStatementLineList())  {
			
			computedBalance = computedBalance.add(bankStatementLine.getAmount());
			
		}
		
		bankStatement.setComputedBalance(computedBalance);
		
		bankStatement.save();
	}
	
	
	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public void validate(BankStatement bankStatement) throws AxelorException  {
		
		this.checkBalance(bankStatement);
		
		for(BankStatementLine bankStatementLine : bankStatement.getBankStatementLineList())  {
			
			if(!bankStatementLine.getIsPosted())  {
			
				if(bankStatementLine.getMoveLine() == null)  {
					this.validate(bankStatementLine);
				}
				else  {
					this.checkAmount(bankStatementLine);
				}
			}
		}
		
		bankStatement.setStatusSelect(BankStatement.STATUS_VALIDATED);
		
		bankStatement.save();
	}
	
	public void checkBalance(BankStatement bankStatement) throws AxelorException  {
		
		if(bankStatement.getComputedBalance().compareTo(bankStatement.getEndingBalance()) != 0)  {  
			throw new AxelorException(String.format(I18n.get(IExceptionMessage.BANK_STATEMENT_1),
					GeneralServiceAccount.getExceptionAccountingMsg()), IException.CONFIGURATION_ERROR);
		}
		
	}
	
	
	public void validate(BankStatementLine bankStatementLine) throws AxelorException  {
		
		BigDecimal amount = bankStatementLine.getAmount();
		
		//TODO add currency conversion
		
		if(amount.compareTo(BigDecimal.ZERO) == 0)  {
			
			return;
			
		}
		
		BankStatement bankStatement = bankStatementLine.getBankStatement();
		
		Partner partner = bankStatementLine.getPartner();
		
		LocalDate effectDate = bankStatementLine.getEffectDate();
		
		String name = bankStatementLine.getName();
		
		Move move = moveService.createMove(bankStatement.getJournal(), bankStatement.getCompany(), null, partner, effectDate, null);
		
		boolean isNegate = amount.compareTo(BigDecimal.ZERO) < 0;
		
		MoveLine partnerMoveLine = moveLineService.createMoveLine(move, partner, bankStatementLine.getAccount(), amount, 
				isNegate, isNegate, effectDate, effectDate, 1, name);
		move.addMoveLineListItem(partnerMoveLine);
		
		move.addMoveLineListItem(
				moveLineService.createMoveLine(move, partner, bankStatement.getCashAccount(), amount, 
						!isNegate, isNegate, effectDate, effectDate, 1, name));

		move.save();
		
		moveService.validateMove(move);
		
		bankStatementLine.setMoveLine(partnerMoveLine);
		
		bankStatementLine.setIsPosted(true);
		
	}
  	
	public void checkAmount(BankStatementLine bankStatementLine) throws AxelorException  {
		
		MoveLine moveLine = bankStatementLine.getMoveLine();
		
		if(bankStatementLine.getAmount().compareTo(BigDecimal.ZERO) == 0 )  {  
			
			throw new AxelorException(String.format(I18n.get(IExceptionMessage.BANK_STATEMENT_3),
					GeneralServiceAccount.getExceptionAccountingMsg(), bankStatementLine.getReference()), IException.CONFIGURATION_ERROR);
		}
		
		if((bankStatementLine.getAmount().compareTo(BigDecimal.ZERO) > 0  && bankStatementLine.getAmount().compareTo(moveLine.getCredit()) != 0 ) 
				|| (bankStatementLine.getAmount().compareTo(BigDecimal.ZERO) < 0  && bankStatementLine.getAmount().compareTo(moveLine.getDebit()) != 0 ) )  {  
			
			throw new AxelorException(String.format(I18n.get(IExceptionMessage.BANK_STATEMENT_2),
					GeneralServiceAccount.getExceptionAccountingMsg(), bankStatementLine.getReference()), IException.CONFIGURATION_ERROR);
		}
		
	}
	
}
