/*******************************************************************************
 * Copyright (c) 2013, 2014 UT-Battelle, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Initial API and implementation and/or initial documentation - 
 *   Kasper Gammeltoft
 *******************************************************************************/
package org.eclipse.ice.datastructures.form;

/**
 * A class to encapsulate a multitude of the same material. This is a much
 * better solution than having a material hold information about its own amount
 * in a stack.
 * 
 * 
 * @author Kasper Gammeltoft
 * 
 */
public class MaterialStack implements Comparable<MaterialStack> {

	/**
	 * The material in this material stack
	 */
	private Material material;

	/**
	 * The amount in this material stack
	 */
	private int number;

	/**
	 * Null constructor.
	 */
	public MaterialStack() {
		material = null;
		number = 0;
	}

	/**
	 * Create a new material stack for holding a material.
	 * 
	 * @param material
	 *            The material to hold.
	 * @param amount
	 *            How many to hold.
	 */
	public MaterialStack(Material material, int amount) {
		this.material = material;
		number = amount;

	}

	/**
	 * Gets the material held in this stack.
	 * 
	 * @return The material
	 */
	public Material getMaterial() {
		return material;
	}

	/**
	 * Sets the material that should be held in this stack. Note- does not reset
	 * the amount held.
	 * 
	 * @param material
	 *            The material to take over the stack.
	 */
	public void setMaterial(Material material) {
		this.material = material;
	}

	/**
	 * The amount of the material held in this stack.
	 * 
	 * @return The amount in the stack
	 */
	public int getAmount() {
		return number;
	}

	/**
	 * Sets the amount of the material in the stack.
	 * 
	 * @param amount
	 *            The amount
	 */
	public void setAmount(int amount) {
		number = amount;
	}

	/**
	 * Adds one to the amount on the stack. A convenience method. The same
	 * effect could be achieved by writing MaterialStack.add(1).
	 */
	public void incrimentAmount() {
		number++;
	}

	/**
	 * Adds the amount specified to the stacks existing amount.
	 * 
	 * @param amount
	 *            The amount to add
	 */
	public void addAmount(int amount) {
		number += amount;
	}

	/**
	 * Compares two material stacks to tell if they are equal to one another.
	 * 
	 * @param stack
	 *            The stack to compare this one with
	 * @return Returns true if the stacks are equal (same material, same amount)
	 *         and false if otherwise.
	 */
	public boolean equals(MaterialStack stack) {
		return number == stack.getAmount()
				&& material.equals(stack.getMaterial());
	}

	/**
	 * Compares two material stacks to see which should be first in a list
	 * containing material stacks. Compares based on the same logic for the
	 * materials, but will list lesser (in amount) material stacks before
	 * greater if the materials are the same when compared.
	 * 
	 * @param
	 * 		stack The material stack to compare to this one.
	 */
	@Override
	public int compareTo(MaterialStack stack) {
		int retVal = 0;
		// The material names are the same. Order from less amount to greater
		// amount.
		if (material.getName().toLowerCase()
				.equals(stack.getMaterial().getName().toLowerCase())) {
			if (number < stack.getAmount()) {
				retVal = -1;
			} else if (number == stack.getAmount()) {
				retVal = 0;
			} else {
				retVal = 1;
			}
		} else {
			retVal = material.compareTo(stack.getMaterial());
		}

		return retVal;
	}

	/**
	 * Gets this material stack as a string. In the format [Material Stack:[ of
	 * Material[some material name] and amount[#]]
	 */
	@Override
	public String toString() {
		String str = "Material Stack:[ of Material[" + material.getName()
				+ "] and amount[" + number + "]";
		return str;
	}

}
