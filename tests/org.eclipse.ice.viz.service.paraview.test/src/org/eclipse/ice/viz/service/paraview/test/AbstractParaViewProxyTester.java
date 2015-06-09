/*******************************************************************************
 * Copyright (c) 2015 UT-Battelle, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jordan H. Deyton (UT-Battelle, LLC.) - Initial API and implementation 
 *   and/or initial documentation
 *   
 *******************************************************************************/
package org.eclipse.ice.viz.service.paraview.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.ice.viz.service.connections.paraview.ParaViewConnectionAdapter;
import org.eclipse.ice.viz.service.paraview.proxy.AbstractParaViewProxy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.kitware.vtk.web.VtkWebClient;

/**
 * This class tests the basic features provided by the
 * {@link AbstractParaViewProxy}.
 * 
 * @author Jordan Deyton
 *
 */
public class AbstractParaViewProxyTester {

	// TODO Check that the initial category, feature, and properties are set.

	/**
	 * The proxy that will be tested.
	 */
	private AbstractParaViewProxy proxy;
	/**
	 * The fake proxy that is used to test the basic implementation provided by
	 * {@link AbstractParaViewProxy}. This should be the same as {@link #proxy}.
	 */
	private FakeParaViewProxy fakeProxy;

	/**
	 * A test URI used to create the {@link #fakeProxy}.
	 */
	private URI testURI;

	/**
	 * A fake ParaView web client. This is the same one contained in
	 * {@link #connection}.
	 */
	private FakeVtkWebClient fakeClient;

	/**
	 * The connection adapter that should be used by the proxy.
	 */
	private ParaViewConnectionAdapter connection;

	/**
	 * Initializes the {@link #proxy}, {@link #fakeProxy}, and {@link #testURI}.
	 */
	@Before
	public void beforeEachTest() {

		// Initialize the proxy with a test URI.
		testURI = TestUtils.createTestURI("go-go-gadget-extension");
		fakeProxy = new FakeParaViewProxy(testURI);
		proxy = fakeProxy;

		// Add some features.
		fakeProxy.features.put("europe", new String[] { "berlin", "madrid",
				"paris", "london", "zagreb" });
		fakeProxy.features.put("north america", new String[] { "ottawa",
				"mexico city", "havanna", "san salvador" });
		// Add some properties.
		fakeProxy.properties.put("south america", new String[] { "bogota",
				"brasilia", "caracas", "buenos aires" });
		fakeProxy.properties.put("africa", new String[] { "johannesburg",
				"cairo", "abuja", "djibouti" });
		fakeProxy.properties.put("asia", new String[] { "ulaanbaatar",
				"beijing", "tokyo", "seoul", "new delhi" });
		fakeProxy.properties.put("australia", new String[] { "canberra" });

		// Set up the fake client.
		fakeClient = new FakeVtkWebClient();

		// Add a test response for creating a view. This is required when
		// "opening" the proxy's file.
		fakeClient.responseMap.put("createView", new Callable<JsonObject>() {
			@Override
			public JsonObject call() throws Exception {
				JsonObject response = new JsonObject();
				response.add("proxyId", new JsonPrimitive(0));
				response.add("viewId", new JsonPrimitive(1));
				response.add("repId", new JsonPrimitive(2));
				return response;
			}
		});

		// Establish a valid ParaView connection that is connected.
		connection = new ParaViewConnectionAdapter() {
			@Override
			protected VtkWebClient openConnection() {
				// Point the connection to localhost.
				setConnectionProperty("host", "localhost");
				// Return the fake client.
				fakeClient.connect("localhost");
				return fakeClient;
			}
		};
		connection.connect(true);

		return;
	}

	/**
	 * Unsets all of the shared test variables.
	 */
	@After
	public void afterEachTest() {
		testURI = null;
		fakeProxy = null;
		proxy = null;
	}

	/**
	 * Checks that {@link AbstractParaViewProxy#getURI()} returns the same URI
	 * as was passed to it.
	 */
	@Test
	public void checkURI() {

		final URI nullURI = null;

		// Check that the URI returned is the same as the one passed into the
		// hidden constructor.
		assertEquals(testURI, proxy.getURI());

		// Trying to use a null URI should throw a NullPointerException.
		try {
			fakeProxy = new FakeParaViewProxy(nullURI);
			fail("AbstractParaViewProxyTester error: "
					+ "A NullPointerException was not thrown when constructed "
					+ "with a null URI.");
		} catch (NullPointerException e) {
			// Exception thrown as expected.
		}

		return;
	}

	/**
	 * Checks that {@link AbstractParaViewProxy#open(ParaViewConnectionAdapter)}
	 * throws exceptions when the arguments are invalid. Also checks that it
	 * correctly calls the implemented open operation.
	 */
	@Test
	public void checkOpen() {

		final ParaViewConnectionAdapter nullConnection = null;

		// Although the client returns nothing (and thus opening fails), the
		// client and URI should pass initial checks, and the open
		// implementation should be called.
		assertTrue(proxy.open(connection));
		assertTrue(fakeProxy.openImplCalled.getAndSet(false));
		// The features and properties should have also been queried.
		assertTrue(fakeProxy.findFeaturesCalled.getAndSet(false));
		assertTrue(fakeProxy.findPropertiesCalled.getAndSet(false));

		// Set a valid connection that is not connected. An exception should not
		// be thrown, but the return value should be false.
		connection = new ParaViewConnectionAdapter();
		assertFalse(proxy.open(connection));
		assertFalse(fakeProxy.openImplCalled.get());
		// The features and properties should not have been queried.
		assertFalse(fakeProxy.findFeaturesCalled.get());
		assertFalse(fakeProxy.findPropertiesCalled.get());

		// Trying to use a null connection should throw an NPE when opening.
		try {
			proxy.open(nullConnection);
			fail("AbstractParaViewProxyTester error: "
					+ "A NullPointerException was not thrown when opened with "
					+ "a null connection.");
		} catch (NullPointerException e) {
			// Exception thrown as expected.
		}
		assertFalse(fakeProxy.openImplCalled.get());

		// TODO Add a test that checks a URI for a different host.
		// TODO Add a test that checks a URI for the same host but specified
		// differently (e.g. FQDN vs IP address).

		return;
	}

	/**
	 * Checks that the abstract implementation for opening a ParaView file works
	 * when it should and gracefully fails when the connection is bad.
	 */
	@Test
	public void checkOpenImplementation() {

		// Initially, the file, view, and representation IDs should be -1.
		assertEquals(-1, fakeProxy.getFileId());
		assertEquals(-1, fakeProxy.getViewId());
		assertEquals(-1, fakeProxy.getRepresentationId());

		// Set a valid connection that is connected. An exception should not be
		// thrown, and the return value should be true.
		assertTrue(proxy.open(connection));

		// Check that the ParaView IDs were set.
		assertEquals(0, fakeProxy.getFileId());
		assertEquals(1, fakeProxy.getViewId());
		assertEquals(2, fakeProxy.getRepresentationId());

		// Set the same valid, open connection again. It should just return
		// true.
		assertTrue(proxy.open(connection));

		// Simulate a failed request (RPC returns a failure).
		fakeClient.responseMap.put("createView", new Callable<JsonObject>() {
			@Override
			public JsonObject call() throws Exception {
				JsonObject response = new JsonObject();
				response.add("success", new JsonPrimitive(false));
				response.add("error", new JsonPrimitive("Simulated error."));
				return response;
			}
		});
		// Opening should return false.
		assertFalse(proxy.open(connection));

		// Simulate a failed request (RPC returns incomplete response).
		fakeClient.responseMap.put("createView", new Callable<JsonObject>() {
			@Override
			public JsonObject call() throws Exception {
				JsonObject response = new JsonObject();
				response.add("proxyId", new JsonPrimitive(10));
				response.add("viewId", new JsonPrimitive(11));
				// repId is omitted... which is an error.
				return response;
			}
		});
		// Opening should return false.
		assertFalse(proxy.open(connection));

		// Simulate a connection error.
		fakeClient.responseMap.put("createView", new Callable<JsonObject>() {
			@Override
			public JsonObject call() throws Exception {
				throw new InterruptedException();
			}
		});
		// Opening should return false.
		assertFalse(proxy.open(connection));

		// The file, view, and representation IDs should remain unchanged.
		assertEquals(0, fakeProxy.getFileId());
		assertEquals(1, fakeProxy.getViewId());
		assertEquals(2, fakeProxy.getRepresentationId());

		return;
	}

	/**
	 * Checks values returned by
	 * {@link AbstractParaViewProxy#getFeatureCategories()} and
	 * {@link AbstractParaViewProxy#getFeatures(String)} as well as exceptions
	 * thrown by the latter method.
	 */
	@Test
	public void checkFeatures() {

		final String nullString = null;

		Set<String> categorySet;
		Set<String> featureSet;

		// Open the proxy. We don't care about its return value, it just must be
		// opened before it finds features for the file.
		proxy.open(connection);
		// The features should have been re-built.
		assertTrue(fakeProxy.findFeaturesCalled.getAndSet(false));

		// Since we know the expected categories and features from the fake
		// proxy's construction, we can just iterate over the maps and sets and
		// make our comparisons.

		// Check the set of categories.
		categorySet = proxy.getFeatureCategories();
		// Check the overall size first.
		assertNotNull(categorySet);
		assertEquals(fakeProxy.features.size(), categorySet.size());
		// Now check each category's features against the expected features.
		for (Entry<String, String[]> entry : fakeProxy.features.entrySet()) {
			// Get the expected category and features.
			String category = entry.getKey();
			String[] features = entry.getValue();

			// Get the set of features for the category, then check its size and
			// content.
			assertTrue(categorySet.contains(category));
			featureSet = proxy.getFeatures(category);
			assertNotNull(featureSet);
			assertEquals(features.length, featureSet.size());
			for (String feature : features) {
				assertTrue(featureSet.contains(feature));
			}
		}

		// Check that new, equivalent sets are returned for both the categories
		// and each category's set of features.
		assertNotSame(categorySet, proxy.getFeatureCategories());
		assertEquals(categorySet, proxy.getFeatureCategories());
		for (String category : categorySet) {
			featureSet = proxy.getFeatures(category);
			assertNotSame(featureSet, proxy.getFeatures(category));
			assertEquals(featureSet, proxy.getFeatures(category));
		}

		// Check that manipulating the returned set of categories or features
		// does not affect the proxy's underlying categories or features.
		proxy.getFeatureCategories().clear();
		categorySet = proxy.getFeatureCategories();
		for (Entry<String, String[]> entry : fakeProxy.features.entrySet()) {
			// Get the expected category and features.
			String category = entry.getKey();
			String[] features = entry.getValue();

			assertTrue(categorySet.contains(category));
			// Try clearing the category's features.
			proxy.getFeatures(category).clear();
			// The feature set for the category should not have changed.
			featureSet = proxy.getFeatures(category);
			assertNotNull(featureSet);
			assertEquals(features.length, featureSet.size());
			for (String feature : features) {
				assertTrue(featureSet.contains(feature));
			}
		}

		// Trying to get the features for a null category should throw an NPE.
		try {
			proxy.getFeatures(nullString);
			fail("AbstractParaViewProxyTester error: "
					+ "When passed a null category, " + "getFeatures(String) "
					+ "should throw a NullPointerException.");
		} catch (NullPointerException e) {
			// Exception thrown as expected.
		}

		// Trying to get the features for an invalid category should throw an
		// IllegalArgumentException.
		try {
			proxy.getFeatures("antarctica");
			fail("AbstractParaViewProxyTester error: "
					+ "When passed an invalid category, "
					+ "getFeatures(String) "
					+ "should throw an IllegalArgumentException.");
		} catch (IllegalArgumentException e) {
			// Exception thrown as expected.
		}

		return;
	}

	/**
	 * Checks that the current feature can be set by calling
	 * {@link AbstractParaViewProxy#setFeature(String, String)} and that the
	 * appropriate exceptions are thrown based on the supplied input.
	 */
	@Test
	public void checkSetFeature() {

		// If set to true, then the render request will "fail" by returning
		// null. Otherwise, it will return an empty JsonObject.
		final AtomicBoolean fail = new AtomicBoolean();
		fakeClient.responseMap.put("pv.color.manager.color.by",
				new Callable<JsonObject>() {
					@Override
					public JsonObject call() throws Exception {
						return fail.get() ? null : new JsonObject();
					}
				});

		final String nullString = null;
		String validCategory;
		String validFeature;

		Set<String> categorySet;
		Set<String> featureSet;

		// Open the proxy. We don't care about its return value, it just must be
		// opened before it finds features for the file.
		proxy.open(connection);

		// Check that all valid categories/features can be set.
		categorySet = proxy.getFeatureCategories();
		for (String category : categorySet) {
			featureSet = proxy.getFeatures(category);
			boolean firstFeature = true;
			for (String feature : featureSet) {

				// If the client fails to render, then the category and feature
				// will not be set, although setFeatureImpl(...) will be called.
				fail.set(true);
				assertFalse(proxy.setFeature(category, feature));
				assertTrue(fakeProxy.setFeatureImplCalled.getAndSet(false));
				if (firstFeature) {
					assertNotEquals(category, fakeProxy.getCategory());
					firstFeature = false;
				}
				assertNotEquals(feature, fakeProxy.getFeature());

				// The first call should successfully set the feature.
				// setFeatureImpl(...) will also be called.
				fail.set(false);
				assertTrue(proxy.setFeature(category, feature));
				assertTrue(fakeProxy.setFeatureImplCalled.getAndSet(false));
				assertEquals(category, fakeProxy.getCategory());
				assertEquals(feature, fakeProxy.getFeature());

				// The second call should return false, because the feature was
				// already set. setFeatureImpl(...) should not be called.
				assertFalse(proxy.setFeature(category, feature));
				assertFalse(fakeProxy.setFeatureImplCalled.get());
			}
		}

		// Get the first valid category/feature from the proxy.
		validCategory = proxy.getFeatureCategories().iterator().next();
		validFeature = proxy.getFeatures(validCategory).iterator().next();

		// Trying to set the feature using a null category should throw an NPE.
		try {
			proxy.setFeature(nullString, validFeature);
			fail("AbstractParaViewProxyTester error: "
					+ "When passed a null category, "
					+ "setFeature(String, String) "
					+ "should throw a NullPointerException.");
		} catch (NullPointerException e) {
			// Exception thrown as expected.
		}

		// Trying to set the feature using a null feature should throw an NPE.
		try {
			proxy.setFeature(validCategory, nullString);
			fail("AbstractParaViewProxyTester error: "
					+ "When passed a null feature, " + ""
					+ "setFeature(String, String) "
					+ "should throw a NullPointerException.");
		} catch (NullPointerException e) {
			// Exception thrown as expected.
		}

		// Trying to set the feature for an invalid category should throw an
		// IllegalArgumentException.
		try {
			proxy.setFeature("antarctica", validFeature);
			fail("AbstractParaViewProxyTester error: "
					+ "When passed an invalid category, "
					+ "setFeature(String, String) "
					+ "should throw a IllegalArgumentException.");
		} catch (IllegalArgumentException e) {
			// Exception thrown as expected.
		}

		// Trying to set the feature to an invalid feature should throw an
		// IllegalArgumentException.
		try {
			proxy.setFeature(validCategory, "international space station");
			fail("AbstractParaViewProxyTester error: "
					+ "When passed an invalid feature, "
					+ "setFeature(String, String) "
					+ "should throw a IllegalArgumentException.");
		} catch (IllegalArgumentException e) {
			// Exception thrown as expected.
		}

		return;
	}

	/**
	 * Checks values returned by {@link AbstractParaViewProxy#getProperties()}.
	 */
	@Test
	public void checkProperties() {

		final String nullString = null;

		Set<String> propertySet;
		Set<String> propertyValueSet;

		// Open the proxy. We don't care about its return value, it just must be
		// opened before it finds properties for the file.
		proxy.open(connection);
		// The properties should have been re-built.
		assertTrue(fakeProxy.findPropertiesCalled.getAndSet(false));

		// Since we know the expected properties and values from the fake
		// proxy's construction, we can just iterate over the maps and sets and
		// make our comparisons.

		// Check the set of properties.
		propertySet = proxy.getProperties();
		// Check the overall size first.
		assertNotNull(propertySet);
		assertEquals(fakeProxy.properties.size(), propertySet.size());
		// Now check each property's values against the expected values.
		for (Entry<String, String[]> entry : fakeProxy.properties.entrySet()) {
			// Get the expected property and values.
			String property = entry.getKey();
			String[] values = entry.getValue();

			// Get the set of values for the property, then check its size and
			// content.
			assertTrue(propertySet.contains(property));
			propertyValueSet = proxy.getPropertyValues(property);
			assertNotNull(propertyValueSet);
			assertEquals(values.length, propertyValueSet.size());
			for (String value : values) {
				assertTrue(propertyValueSet.contains(value));
			}
		}

		// Check that new, equivalent sets are returned for both the properties
		// and each property's set of allowed values.
		assertNotSame(propertySet, proxy.getProperties());
		assertEquals(propertySet, proxy.getProperties());
		for (String property : propertySet) {
			propertyValueSet = proxy.getPropertyValues(property);
			assertNotSame(propertyValueSet, proxy.getPropertyValues(property));
			assertEquals(propertyValueSet, proxy.getPropertyValues(property));
		}

		// Check that manipulating the returned set of properties or values
		// does not affect the proxy's underlying properties or values.
		proxy.getProperties().clear();
		propertySet = proxy.getProperties();
		for (Entry<String, String[]> entry : fakeProxy.properties.entrySet()) {
			// Get the expected property and values.
			String property = entry.getKey();
			String[] values = entry.getValue();

			assertTrue(propertySet.contains(property));
			// Try clearing the property's values.
			proxy.getPropertyValues(property).clear();
			// The value set for the property should not have changed.
			propertyValueSet = proxy.getPropertyValues(property);
			assertNotNull(propertyValueSet);
			assertEquals(values.length, propertyValueSet.size());
			for (String value : values) {
				assertTrue(propertyValueSet.contains(value));
			}
		}

		// Trying to get the values for a null property should throw an NPE.
		try {
			proxy.getFeatures(nullString);
			fail("AbstractParaViewProxyTester error: "
					+ "When passed a null property, "
					+ "getPropertyValues(String) "
					+ "should throw a NullPointerException.");
		} catch (NullPointerException e) {
			// Exception thrown as expected.
		}

		// Trying to get the values for an invalid property should throw an
		// IllegalArgumentException.
		try {
			proxy.getFeatures("antarctica");
			fail("AbstractParaViewProxyTester error: "
					+ "When passed an invalid property, "
					+ "getPropertyValues(String) "
					+ "should throw an IllegalArgumentException.");
		} catch (IllegalArgumentException e) {
			// Exception thrown as expected.
		}

		return;
	}

	/**
	 * Checks that a single property can be set via
	 * {@link AbstractParaViewProxy#setProperty(String, String)} and that the
	 * appropriate exceptions are thrown based on the supplied input.
	 */
	@Test
	public void checkSetProperty() {

		final String nullString = null;
		String validProperty;
		String validValue;

		Set<String> propertySet;
		Set<String> valueSet;

		// Open the proxy. We don't care about its return value, it just must be
		// opened before it finds properties for the file.
		proxy.open(connection);

		// Check that all valid properties/values can be set.
		propertySet = proxy.getProperties();
		for (String property : propertySet) {
			valueSet = proxy.getPropertyValues(property);
			for (String value : valueSet) {
				// If the client fails to update the property, then the property
				// value will not be set, although setPropertyImpl(...) will
				// still be called.
				fakeProxy.failToSetProperty = true;
				assertFalse(proxy.setProperty(property, value));
				assertTrue(fakeProxy.setPropertyImplCalled.getAndSet(false));
				assertNotEquals(value, fakeProxy.getPropertyValue(property));

				// The first call should successfully set the property.
				// setPropertyImpl(...) will also be called.
				fakeProxy.failToSetProperty = false;
				assertTrue(proxy.setProperty(property, value));
				assertTrue(fakeProxy.setPropertyImplCalled.getAndSet(false));
				assertEquals(value, fakeProxy.getPropertyValue(property));

				// The second call should return false, because the property was
				// already set.
				assertFalse(proxy.setProperty(property, value));
				assertFalse(fakeProxy.setFeatureImplCalled.get());
			}
		}

		// Get the first valid property/value from the proxy.
		validProperty = proxy.getProperties().iterator().next();
		validValue = proxy.getPropertyValues(validProperty).iterator().next();

		// Trying to set the property value using a null property should throw
		// an NPE.
		try {
			proxy.setProperty(nullString, validValue);
			fail("AbstractParaViewProxyTester error: "
					+ "When passed a null property, "
					+ "setProperty(String, String) "
					+ "should throw a NullPointerException.");
		} catch (NullPointerException e) {
			// Exception thrown as expected.
		}

		// Trying to set the property value using a null value should throw an
		// NPE.
		try {
			proxy.setProperty(validProperty, nullString);
			fail("AbstractParaViewProxyTester error: "
					+ "When passed a null value, " + ""
					+ "setProperty(String, String) "
					+ "should throw a NullPointerException.");
		} catch (NullPointerException e) {
			// Exception thrown as expected.
		}

		// Trying to set the property value for an invalid property should throw
		// an IllegalArgumentException.
		try {
			proxy.setProperty("antarctica", validValue);
			fail("AbstractParaViewProxyTester error: "
					+ "When passed an invalid property, "
					+ "setProperty(String, String) "
					+ "should throw a IllegalArgumentException.");
		} catch (IllegalArgumentException e) {
			// Exception thrown as expected.
		}

		// Trying to set the property value to an invalid value should throw an
		// IllegalArgumentException.
		try {
			proxy.setProperty(validProperty, "international space station");
			fail("AbstractParaViewProxyTester error: "
					+ "When passed an invalid value, "
					+ "setProperty(String, String) "
					+ "should throw a IllegalArgumentException.");
		} catch (IllegalArgumentException e) {
			// Exception thrown as expected.
		}

		return;
	}

	/**
	 * Checks that a set of properties can be set via
	 * {@link AbstractParaViewProxy#setProperties(java.util.Map)} and that the
	 * appropriate exceptions are thrown based on the supplied input.
	 */
	@Test
	public void checkSetProperties() {

		final String nullString = null;
		String validProperty;
		String validValue;

		// Set up a map of properties that will be set on the proxy.
		final Map<String, String> newProperties = new HashMap<String, String>();
		final Map<String, String> newPropertiesCopy;

		// Open the proxy. We don't care about its return value, it just must be
		// opened before it finds properties for the file.
		proxy.open(connection);

		// Add 3 properties where 2 of them are new values and one is old.
		if ("djibouti".equals(fakeProxy.getPropertyValue("africa"))) {
			newProperties.put("africa", "abuja");
		} else {
			newProperties.put("africa", "djibouti");
		}
		if ("tokyo".equals(fakeProxy.getPropertyValue("asia"))) {
			newProperties.put("asia", "beijing");
		} else {
			newProperties.put("asia", "tokyo");
		}
		newProperties.put("australia", "canberra"); // The old value.

		// Get the first valid property/value from the proxy.
		validProperty = proxy.getProperties().iterator().next();
		validValue = proxy.getPropertyValues(validProperty).iterator().next();

		// If any of the new property names are null, an NPE will be thrown.
		newProperties.put(nullString, validValue);
		try {
			proxy.setProperties(newProperties);
			fail("AbstractParaViewProxyTester error: "
					+ "When passed a null property, "
					+ "setProperties(Map<String, String>) "
					+ "should throw a NullPointerException.");
		} catch (NullPointerException e) {
			// Exception thrown as expected.
		}
		// Remove the property that we attempted to update.
		newProperties.remove(nullString);

		// If any of the new property values are null, an NPE will be thrown.
		newProperties.put(validProperty, nullString);
		try {
			proxy.setProperties(newProperties);
			fail("AbstractParaViewProxyTester error: "
					+ "When passed a null value, " + ""
					+ "setProperties(Map<String, String>) "
					+ "should throw a NullPointerException.");
		} catch (NullPointerException e) {
			// Exception thrown as expected.
		}
		// Remove the property that we attempted to update.
		newProperties.remove(validProperty);

		// If any of the new property names are invalid, an
		// IllegalArgumentException will be thrown.
		newProperties.put("antarctica", validValue);
		try {
			proxy.setProperties(newProperties);
			fail("AbstractParaViewProxyTester error: "
					+ "When passed an invalid property, "
					+ "setProperties(Map<String, String>) "
					+ "should throw a IllegalArgumentException.");
		} catch (IllegalArgumentException e) {
			// Exception thrown as expected.
		}
		// Remove the property that we attempted to update.
		newProperties.remove("antarctica");

		// If any of the new property values are invalid, an
		// IllegalArgumentException will be thrown.
		newProperties.put(validProperty, "international space station");
		try {
			proxy.setProperties(newProperties);
			fail("AbstractParaViewProxyTester error: "
					+ "When passed an invalid value, "
					+ "setProperties(Map<String, String>) "
					+ "should throw a IllegalArgumentException.");
		} catch (IllegalArgumentException e) {
			// Exception thrown as expected.
		}
		// Remove the property that we attempted to update.
		newProperties.remove(validProperty);

		// Make sure the remaining valid properties were never set by sending
		// the new properties minus all of the invalid properties.
		newPropertiesCopy = new HashMap<String, String>(newProperties);
		assertEquals(2, proxy.setProperties(newProperties));

		// Since the properties were updated, sending the same set of properties
		// should return 0 (for 0 properties changed).
		assertEquals(0, proxy.setProperties(newPropertiesCopy));

		return;
	}

	/**
	 * A fake proxy that extends {@link AbstractParaViewProxy} and exposes
	 * certiain methods to ensure the abstract class re-directs method calls
	 * when appropriate to its sub-classes.
	 * 
	 * @author Jordan Deyton
	 *
	 */
	private class FakeParaViewProxy extends AbstractParaViewProxy {

		/**
		 * A map of supported categories and features for the fake proxy. This
		 * should be set at construction time.
		 */
		public final Map<String, String[]> features;
		/**
		 * A map of supported properties and their allowed values for the fake
		 * proxy. This should be set at construction time.
		 */
		public final Map<String, String[]> properties;

		/**
		 * Whether or not {@link #openImpl(VtkWebClient, String)} was called.
		 */
		public final AtomicBoolean openImplCalled = new AtomicBoolean();
		/**
		 * Whether or not {@link #findFeatures(VtkWebClient)} was called.
		 */
		public final AtomicBoolean findFeaturesCalled = new AtomicBoolean();
		/**
		 * Whether or not {@link #findProperties(VtkWebClient)} was called.
		 */
		public final AtomicBoolean findPropertiesCalled = new AtomicBoolean();
		/**
		 * Whether or not {@link #setFeatureImpl(VtkWebClient, String, String)}
		 * was called.
		 */
		public final AtomicBoolean setFeatureImplCalled = new AtomicBoolean();
		/**
		 * Whether or not {@link #setPropertyImpl(VtkWebClient, String, String)}
		 * was called.
		 */
		public final AtomicBoolean setPropertyImplCalled = new AtomicBoolean();

		/**
		 * If true, then {@link #setPropertyImpl(VtkWebClient, String, String)}
		 * will "fail" and return false, otherwise it will "succeed" and return
		 * true.
		 */
		public boolean failToSetProperty = false;

		/**
		 * The default constructor. Used to access the parent class' hidden
		 * constructor (after all, it is an abstract class).
		 * 
		 * @param uri
		 *            The URI for the ParaView-supported file.
		 * @throws NullPointerException
		 *             If the specified URI is null.
		 */
		public FakeParaViewProxy(URI uri) throws NullPointerException {
			super(uri);

			features = new HashMap<String, String[]>();
			properties = new HashMap<String, String[]>();
		}

		/**
		 * Exposes the parent class' operation.
		 */
		public int getFileId() {
			return super.getFileId();
		}

		/**
		 * Exposes the parent class' operation.
		 */
		public int getViewId() {
			return super.getViewId();
		}

		/**
		 * Exposes the parent class' operation.
		 */
		public int getRepresentationId() {
			return super.getRepresentationId();
		}

		/**
		 * Exposes the parent class' operation.
		 */
		@Override
		public String getCategory() {
			return super.getCategory();
		}

		/**
		 * Exposes the parent class' operation.
		 */
		@Override
		public String getFeature() {
			return super.getFeature();
		}

		/**
		 * Exposes the parent class' operation.
		 */
		@Override
		public String getPropertyValue(String property) {
			return super.getPropertyValue(property);
		}

		/**
		 * Overrides the default behavior to additionally set
		 * {@link #openImplCalled} to true when called.
		 */
		public boolean openImpl(VtkWebClient client, String fullPath) {
			openImplCalled.set(true);
			return super.openImpl(client, fullPath);
		}

		/*
		 * Overrides a method from AbstractParaViewProxy.
		 */
		@Override
		protected Map<String, String[]> findFeatures(VtkWebClient client) {
			findFeaturesCalled.set(true);
			return features;
		}

		/*
		 * Overrides a method from AbstractParaViewProxy.
		 */
		@Override
		protected Map<String, String[]> findProperties(VtkWebClient client) {
			findPropertiesCalled.set(true);
			return properties;
		}

		/**
		 * Overrides the default behavior to additionally set
		 * {@link #setFeatureImplCalled} to true.
		 */
		@Override
		protected boolean setFeatureImpl(VtkWebClient client, String category,
				String feature) {
			setFeatureImplCalled.set(true);
			return super.setFeatureImpl(client, category, feature);
		}

		/**
		 * Sets {@link #setPropertyImplCalled} to true. Returns true if
		 * {@link #failToSetProperty} is false, false otherwise.
		 */
		@Override
		protected boolean setPropertyImpl(VtkWebClient client, String property,
				String value) {
			setPropertyImplCalled.set(true);
			return !failToSetProperty;
		}
	}
}