/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.totschnig.myexpenses;

import java.sql.Timestamp;
import java.util.Date;

import android.database.Cursor;

/**
 * Domain class for transactions
 * @author Michael Totschnig
 *
 */
public class Transaction {
  public long id = 0;
  public String comment;
  public Date date;
  public float amount;
  //for transfers cat_id stores the peer account
  public long cat_id;
  //stores a short label of the category or the account the transaction is linked to
  public String label;
  public long account_id;
  public String payee;
  public long transfer_peer = 0;
  /**
   * we store the date directly from UI to DB without creating a Date object
   */
  protected String dateAsString;
  protected ExpensesDbAdapter mDbHelper;
  
  /**
   * factory method for retrieving an instance from the db with the give id
   * @param mDbHelper
   * @param id
   * @return instance of {@link Transaction} or {@link Transfer}
   */
  static Transaction getInstanceFromDb(ExpensesDbAdapter mDbHelper, long id) {
    Transaction t;
    Cursor c = mDbHelper.fetchExpense(id);
    long transfer_peer = c.getLong(c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_TRANSFER_PEER));
    if (transfer_peer == 0)
      t = new Transaction(mDbHelper,id,c);
    else
      t = new Transfer(mDbHelper,id,c);
    c.close();
    return t;
  }
  /**
   * factory method for creating an object of the correct type
   * @param mDbHelper
   * @param mOperationType either {@link MyExpenses#TYPE_TRANSACTION} or
   * {@link MyExpenses#TYPE_TRANSFER}
   * @return instance of {@link Transaction} or {@link Transfer}
   */
  public static Transaction getTypedNewInstance(ExpensesDbAdapter mDbHelper,
      boolean mOperationType) {
    if(mOperationType == MyExpenses.TYPE_TRANSACTION)
      return new Transaction(mDbHelper);
    else 
      return new Transfer(mDbHelper);
  }
  
  /**
   * new empty transaction
   * @param mDbHelper
   */
  public Transaction(ExpensesDbAdapter mDbHelper) {
    this.mDbHelper = mDbHelper;
    this.date = new Date();
  }
  /**
   * transaction instace from db using the provided cursor
   * @param mDbHelper
   * @param id
   * @param c since the {@link #getInstanceFromDb(ExpensesDbAdapter, long) factory method}
   * already opens the cursor we accept it as input here
   */
  public Transaction(ExpensesDbAdapter mDbHelper, long id, Cursor c) {
    this.mDbHelper = mDbHelper;
    this.id = id;
    dateAsString = c.getString(
        c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_DATE));
    date = Timestamp.valueOf(dateAsString);
    try {
      amount = Float.valueOf(c.getString(
          c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_AMOUNT)));
    } catch (NumberFormatException e) {
      amount = 0;
    }
    comment = c.getString(
        c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_COMMENT));
    payee = c.getString(
            c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_PAYEE));
    cat_id = c.getLong(c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_CATID));
    transfer_peer = c.getLong(c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_TRANSFER_PEER));
    label = c.getString(c.getColumnIndexOrThrow("label"));
    account_id = c.getLong(c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_ACCOUNTID));
  }
  /**
   * we store the date string and create a date object from it
   * @param strDate format accepted by {@link Timestamp#valueOf}
   */
  public void setDate(String strDate) {
    //as a temporary shortcut we store the date as string,
    //since we have tested that this way UI->DB works
    //and have no time at the moment to test detour via Date class
    dateAsString = strDate;
    date = Timestamp.valueOf(strDate);
  }
  /**
   * as a side effect calls {@link ExpensesDbAdapter#createPayeeOrIgnore(String)}
   * @param payee
   */
  public void setPayee(String payee) {
    this.payee = payee;
    mDbHelper.createPayeeOrIgnore(payee);
  }
  /**
   * Saves the transaction, creating it new if necessary
   * @return the id of the transaction. Upon creation it is returned from the database
   */
  public long save() {
    if (id == 0) {
      id = mDbHelper.createExpense(dateAsString, amount, comment,cat_id,account_id,payee);
    } else {
      mDbHelper.updateExpense(id, dateAsString, amount, comment,cat_id,payee);
    }
    return id;
  }


}
