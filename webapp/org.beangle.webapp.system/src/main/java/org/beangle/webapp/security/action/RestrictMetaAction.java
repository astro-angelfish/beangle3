/* Copyright c 2005-2012.
 * Licensed under GNU  LESSER General Public License, Version 3.
 * http://www.gnu.org/licenses
 */
package org.beangle.webapp.security.action;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.beangle.commons.collection.CollectUtils;
import org.beangle.commons.lang.StrUtils;
import org.beangle.model.query.builder.OqlBuilder;
import org.beangle.security.blueprint.restrict.RestrictField;
import org.beangle.security.blueprint.restrict.RestrictEntity;
import org.beangle.security.blueprint.restrict.RestrictPattern;

public class RestrictMetaAction extends SecurityActionSupport {

	public String patterns() {
		OqlBuilder<RestrictPattern> query = OqlBuilder.from(RestrictPattern.class, "pattern");
		populateConditions(query);
		query.orderBy(get("orderBy")).limit(getPageLimit());
		put("patterns", search(query));
		return forward();
	}

	public String fields() {
		put("fields", search(getFieldQueryBuilder()));
		put("entities", entityDao.getAll(RestrictEntity.class));
		return forward();
	}

	private OqlBuilder<RestrictField> getFieldQueryBuilder() {
		OqlBuilder<RestrictField> query = OqlBuilder.from(RestrictField.class, "field");
		populateConditions(query);
		query.orderBy(get("orderBy")).limit(getPageLimit());
		Long entityId = getLong("entity.id");
		if (null != entityId) {
			query.join("field.entities", "entity");
			query.where("entity.id=:entityId", entityId);
		}
		return query;
	}

	public String patternInfo() {
		put("pattern", getEntity(RestrictPattern.class, "pattern"));
		return forward();
	}

	public String editPattern() {
		RestrictPattern pattern = getEntity(RestrictPattern.class, "pattern");
		put("pattern", pattern);
		put("entities", entityDao.getAll(RestrictEntity.class));
		return forward("patternForm");
	}

	public String savePattern() {
		RestrictPattern pattern = populateEntity(RestrictPattern.class, "pattern");
		entityDao.saveOrUpdate(pattern);
		return redirect("patterns", "info.save.success");
	}

	public String saveEntity() {
		RestrictEntity group = (RestrictEntity) populateEntity(RestrictEntity.class, "entity");
		entityDao.saveOrUpdate(group);
		logger.info("save restrict entity with name {}", group.getName());
		return redirect("index", "info.save.success");
	}

	public String removeEntity() {
		Long groupId = getEntityId("entity");
		if (null != groupId) {
			RestrictEntity group = (RestrictEntity) entityDao.get(RestrictEntity.class, groupId);
			entityDao.remove(group);
			logger.info("remove group with name {}", group.getName());
		}
		return redirect("index", "info.remove.success");
	}

	public String editField() {
		RestrictField field = getEntity(RestrictField.class, "field");
		List<RestrictEntity> entities = entityDao.getAll(RestrictEntity.class);
		entities.removeAll(field.getEntities());
		put("entities", entities);
		put("field", field);
		return forward("fieldForm");
	}

	public String saveField() {
		String entityIds = get("entityIds");
		List<RestrictEntity> paramGroups = CollectUtils.newArrayList();
		if (StringUtils.isNotBlank(entityIds)) {
			paramGroups = entityDao.get(RestrictEntity.class, StrUtils.splitToLong(entityIds));
		}
		RestrictField field = populateEntity(RestrictField.class, "field");
		field.getEntities().clear();
		field.getEntities().addAll(paramGroups);
		saveOrUpdate(field);
		return redirect("fields", "info.save.success");
	}

}
