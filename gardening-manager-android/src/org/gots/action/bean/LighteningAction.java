/*******************************************************************************
 * Copyright (c) 2012 sfleury.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     sfleury - initial API and implementation
 ******************************************************************************/
package org.gots.action.bean;

import java.util.Calendar;
import java.util.Date;

import org.gots.action.AbstractActionSeed;
import org.gots.action.ActionOnSeed;
import org.gots.seed.GrowingSeed;

import android.content.Context;

public class LighteningAction extends AbstractActionSeed implements ActionOnSeed {

	public LighteningAction(	Context mContext) {
	    super(mContext);
		setName("lighten");
	}

}
