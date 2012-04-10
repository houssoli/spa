package de.codecentric.spa;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import de.codecentric.spa.ctx.PersistenceApplicationContext;
import de.codecentric.spa.metadata.EntityMetaData;
import de.codecentric.spa.metadata.FieldMetaData;
import de.codecentric.spa.metadata.RelationshipMetaData;
import de.codecentric.spa.metadata.RelationshipMetaDataProvider;

/**
 * Content values helper class - it provides methods for supplying given
 * {@link ContentValues} with value of proper type from given object and its
 * field.
 */
public class ContentValuesPreparer {

	@SuppressWarnings("rawtypes")
	private EntityHelper entityHelper;
	private PersistenceApplicationContext context;

	@SuppressWarnings("rawtypes")
	public ContentValuesPreparer(EntityHelper entityHelper) {
		this.entityHelper = entityHelper;
		this.context = entityHelper.getPersistenceApplicationContext();
	}

	/**
	 * Method returns {@link ContentValues} object filled with column names and
	 * values.
	 * 
	 * NOTE: method does not work properly with byte[] parameters.
	 * 
	 * @param object
	 *            object which mapping is done
	 * @param emd
	 *            entity meta data
	 * @return {@link ContentValues} object filled with column names and values
	 */
	public ContentValues prepareValues(final Object object, EntityMetaData emd) {
		ContentValues values = new ContentValues();

		try {

			List<FieldMetaData> mFieldList = emd.getPersistentFields();
			if (mFieldList != null && !mFieldList.isEmpty()) {

				for (FieldMetaData mFld : mFieldList) {
					Field dataFld = null;
					try {
						dataFld = object.getClass().getField(mFld.getFieldName());
						prepareSingleValue(values, object, mFld, dataFld);
					} catch (NoSuchFieldException e) {
						// If the field is not found, we should try a search
						// through complex structure.
						// Try to find a field of it's type and than field of
						// given name in substructure.
						Field[] flds = object.getClass().getFields();
						Object particle = null;
						for (Field f : flds) {
							Class<?> particleClass = f.getType();
							if (particleClass.equals(mFld.getDeclaringClass())) {
								dataFld = particleClass.getField(mFld.getFieldName());
								particle = f.get(object);
								break;
							}
						}
						if (dataFld != null) {
							prepareSingleValue(values, particle, mFld, dataFld);
						}
					}
				}

			}

			prepareRelationshipFieldValues(values, object);

		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

		return values;
	}

	/**
	 * Method puts single value (determined by given meta field and data field
	 * from given object) into the values map.
	 * 
	 * @param values
	 * @param object
	 * @param mFld
	 * @param dataFld
	 * @throws Exception
	 */
	private void prepareSingleValue(final ContentValues values, final Object object, FieldMetaData mFld, Field dataFld)
			throws Exception {
		String typeName = dataFld.getType().getName();

		if (boolean.class.getName().equals(typeName) || Boolean.class.getName().equals(typeName)) {

			prepareBooleanValue(values, object, mFld, dataFld);

		} else if (Date.class.getName().equals(typeName)) {

			prepareDateValue(values, object, mFld, dataFld);

		} else if (byte[].class.getName().equals(typeName)) {

			// TODO

		} else if (double.class.getName().equals(typeName) || Double.class.getName().equals(typeName)) {

			prepareDoubleValue(values, object, mFld, dataFld);

		} else if (float.class.getName().equals(typeName) || Float.class.getName().equals(typeName)) {

			prepareFloatValue(values, object, mFld, dataFld);

		} else if (int.class.getName().equals(typeName) || Integer.class.getName().equals(typeName)) {

			prepareIntegerValue(values, object, mFld, dataFld);

		} else if (long.class.getName().equals(typeName) || Long.class.getName().equals(typeName)) {

			prepareLongValue(values, object, mFld, dataFld);

		} else if (short.class.getName().equals(typeName) || Short.class.getName().equals(typeName)) {

			prepareShortValue(values, object, mFld, dataFld);

		} else if (byte.class.getName().equals(typeName) || Byte.class.getName().equals(typeName)) {

			prepareByteValue(values, object, mFld, dataFld);

		} else {

			prepareStringValue(values, object, mFld, dataFld);

		}

	}

	/**
	 * Method puts relationship values into the values map.
	 * 
	 * @param values
	 * @param object
	 */
	@SuppressWarnings("unchecked")
	private void prepareRelationshipFieldValues(final ContentValues values, final Object object) {
		// Go through relationship meta data of given entity's class...
		List<RelationshipMetaData> rMFieldList = RelationshipMetaDataProvider.getInstance().getMetaDataByChild(
				object.getClass());
		if (rMFieldList != null && !rMFieldList.isEmpty()) {
			EntityTransactionCache eCache = EntityTransactionCache.getInstance();
			for (RelationshipMetaData rmd : rMFieldList) {
				Class<?> parentClass = rmd.getParentClass();
				// Try to find parent entity in entity transaction cache and
				// use it's values.
				Object parentEntity = eCache.read(parentClass);
				if (parentEntity != null) {
					values.put(rmd.getForeignKeyColumnName(), entityHelper.getIdentifierValue(parentEntity,
							new EntityHelper<Class<?>>(context, parentEntity.getClass())));
				}
			}
		}
	}

	private void prepareBooleanValue(final ContentValues values, final Object object, FieldMetaData mFld, Field dataFld)
			throws Exception {
		Boolean value = (Boolean) dataFld.get(object);
		if (value != null) {
			values.put(mFld.getColumnName(), value ? 1 : 0);
		} else {
			values.put(mFld.getColumnName(), 0);
		}
	}

	private void prepareDateValue(final ContentValues values, final Object object, FieldMetaData mFld, Field dataFld)
			throws Exception {
		Date value = (Date) dataFld.get(object);
		values.put(mFld.getColumnName(), value != null ? value.getTime() : null);
	}

	private void prepareDoubleValue(final ContentValues values, final Object object, FieldMetaData mFld, Field dataFld)
			throws Exception {
		Double value = (Double) dataFld.get(object);
		values.put(mFld.getColumnName(), value != null ? value : null);
	}

	private void prepareFloatValue(final ContentValues values, final Object object, FieldMetaData mFld, Field dataFld)
			throws Exception {
		Float value = (Float) dataFld.get(object);
		values.put(mFld.getColumnName(), value != null ? value : null);
	}

	private void prepareIntegerValue(final ContentValues values, final Object object, FieldMetaData mFld, Field dataFld)
			throws Exception {
		Integer value = (Integer) dataFld.get(object);
		values.put(mFld.getColumnName(), value != null ? value : null);
	}

	private void prepareLongValue(final ContentValues values, final Object object, FieldMetaData mFld, Field dataFld)
			throws Exception {
		Long value = (Long) dataFld.get(object);
		values.put(mFld.getColumnName(), value != null ? value : null);
	}

	private void prepareShortValue(final ContentValues values, final Object object, FieldMetaData mFld, Field dataFld)
			throws Exception {
		Short value = (Short) dataFld.get(object);
		values.put(mFld.getColumnName(), value != null ? value : null);
	}

	private void prepareByteValue(final ContentValues values, final Object object, FieldMetaData mFld, Field dataFld)
			throws Exception {
		Byte value = (Byte) dataFld.get(object);
		values.put(mFld.getColumnName(), value != null ? value : null);
	}

	private void prepareStringValue(final ContentValues values, final Object object, FieldMetaData mFld, Field dataFld)
			throws Exception {
		String value = (String) dataFld.get(object);
		values.put(mFld.getColumnName(), value != null ? value : null);
	}

}