/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.olingo.client.core.serialization;

import java.io.StringWriter;
import java.net.URI;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.olingo.client.api.EdmEnabledODataClient;
import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.client.api.data.ServiceDocument;
import org.apache.olingo.client.api.data.ServiceDocumentItem;
import org.apache.olingo.client.api.serialization.ODataBinder;
import org.apache.olingo.client.core.uri.URIUtils;
import org.apache.olingo.commons.api.Constants;
import org.apache.olingo.commons.api.data.Annotatable;
import org.apache.olingo.commons.api.data.Annotation;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.DeletedEntity;
import org.apache.olingo.commons.api.data.Delta;
import org.apache.olingo.commons.api.data.DeltaLink;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntitySet;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.data.Linked;
import org.apache.olingo.commons.api.data.LinkedComplexValue;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ResWrap;
import org.apache.olingo.commons.api.data.Valuable;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.domain.ODataAnnotatable;
import org.apache.olingo.commons.api.domain.ODataAnnotation;
import org.apache.olingo.commons.api.domain.ODataCollectionValue;
import org.apache.olingo.commons.api.domain.ODataComplexValue;
import org.apache.olingo.commons.api.domain.ODataDeletedEntity.Reason;
import org.apache.olingo.commons.api.domain.ODataDelta;
import org.apache.olingo.commons.api.domain.ODataDeltaLink;
import org.apache.olingo.commons.api.domain.ODataEntity;
import org.apache.olingo.commons.api.domain.ODataEntitySet;
import org.apache.olingo.commons.api.domain.ODataInlineEntity;
import org.apache.olingo.commons.api.domain.ODataInlineEntitySet;
import org.apache.olingo.commons.api.domain.ODataLink;
import org.apache.olingo.commons.api.domain.ODataLinkType;
import org.apache.olingo.commons.api.domain.ODataLinked;
import org.apache.olingo.commons.api.domain.ODataLinkedComplexValue;
import org.apache.olingo.commons.api.domain.ODataOperation;
import org.apache.olingo.commons.api.domain.ODataProperty;
import org.apache.olingo.commons.api.domain.ODataServiceDocument;
import org.apache.olingo.commons.api.domain.ODataValuable;
import org.apache.olingo.commons.api.domain.ODataValue;
import org.apache.olingo.commons.api.edm.Edm;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;
import org.apache.olingo.commons.api.edm.EdmComplexType;
import org.apache.olingo.commons.api.edm.EdmElement;
import org.apache.olingo.commons.api.edm.EdmEntityContainer;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmEnumType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.EdmSchema;
import org.apache.olingo.commons.api.edm.EdmStructuredType;
import org.apache.olingo.commons.api.edm.EdmTerm;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.geo.Geospatial;
import org.apache.olingo.commons.api.format.ODataFormat;
import org.apache.olingo.commons.api.serialization.ODataSerializerException;
import org.apache.olingo.commons.core.data.AnnotationImpl;
import org.apache.olingo.commons.core.data.EntityImpl;
import org.apache.olingo.commons.core.data.EntitySetImpl;
import org.apache.olingo.commons.core.data.LinkImpl;
import org.apache.olingo.commons.core.data.LinkedComplexValueImpl;
import org.apache.olingo.commons.core.data.PropertyImpl;
import org.apache.olingo.commons.core.domain.ODataAnnotationImpl;
import org.apache.olingo.commons.core.domain.ODataDeletedEntityImpl;
import org.apache.olingo.commons.core.domain.ODataDeltaLinkImpl;
import org.apache.olingo.commons.core.domain.ODataPropertyImpl;
import org.apache.olingo.commons.core.edm.EdmTypeInfo;
import org.apache.olingo.commons.core.edm.primitivetype.EdmPrimitiveTypeFactory;
import org.apache.olingo.commons.core.serialization.ContextURLParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ODataBinderImpl implements ODataBinder {

  /**
   * Logger.
   */
  protected static final Logger LOG = LoggerFactory.getLogger(ODataBinderImpl.class);

  protected final ODataClient client;

  public ODataBinderImpl(final ODataClient client) {
    this.client = client;
  }

  @Override
  public boolean add(final ODataEntity entity, final ODataProperty property) {
    return ((ODataEntity) entity).getProperties().add((ODataProperty) property);
  }

  protected boolean add(final ODataEntitySet entitySet, final ODataEntity entity) {
    return ((ODataEntitySet) entitySet).getEntities().add((ODataEntity) entity);
  }

  @Override
  public ODataServiceDocument getODataServiceDocument(final ServiceDocument resource) {
    final ODataServiceDocument serviceDocument = new ODataServiceDocument();

    for (ServiceDocumentItem entitySet : resource.getEntitySets()) {
      serviceDocument.getEntitySets().
          put(entitySet.getName(), URIUtils.getURI(resource.getBaseURI(), entitySet.getUrl()));
    }
    for (ServiceDocumentItem functionImport : resource.getFunctionImports()) {
      serviceDocument.getFunctionImports().put(
          functionImport.getName() == null ? functionImport.getUrl() : functionImport.getName(),
          URIUtils.getURI(resource.getBaseURI(), functionImport.getUrl()));
    }
    for (ServiceDocumentItem singleton : resource.getSingletons()) {
      serviceDocument.getSingletons().put(
          singleton.getName() == null ? singleton.getUrl() : singleton.getName(),
          URIUtils.getURI(resource.getBaseURI(), singleton.getUrl()));
    }
    for (ServiceDocumentItem sdoc : resource.getRelatedServiceDocuments()) {
      serviceDocument.getRelatedServiceDocuments().put(
          sdoc.getName() == null ? sdoc.getUrl() : sdoc.getName(),
          URIUtils.getURI(resource.getBaseURI(), sdoc.getUrl()));
    }

    return serviceDocument;
  }

  private void updateValuable(final Valuable propertyResource, final ODataValuable odataValuable) {
    final Object propertyValue = getValue(odataValuable.getValue());
    if (odataValuable.hasPrimitiveValue()) {
      propertyResource.setType(odataValuable.getPrimitiveValue().getTypeName());
      propertyResource.setValue(
          propertyValue instanceof Geospatial ? ValueType.GEOSPATIAL : ValueType.PRIMITIVE,
          propertyValue);
    } else if (odataValuable.hasEnumValue()) {
      propertyResource.setType(odataValuable.getEnumValue().getTypeName());
      propertyResource.setValue(ValueType.ENUM, propertyValue);
    } else if (odataValuable.hasComplexValue()) {
      propertyResource.setType(odataValuable.getComplexValue().getTypeName());
      propertyResource.setValue(
          propertyValue instanceof LinkedComplexValue ? ValueType.LINKED_COMPLEX : ValueType.COMPLEX,
          propertyValue);
    } else if (odataValuable.hasCollectionValue()) {
      final ODataCollectionValue<org.apache.olingo.commons.api.domain.ODataValue> collectionValue =
          odataValuable.getCollectionValue();
      propertyResource.setType(collectionValue.getTypeName());
      final org.apache.olingo.commons.api.domain.ODataValue value =
          collectionValue.iterator().hasNext() ? collectionValue.iterator().next() : null;
      ValueType valueType = ValueType.COLLECTION_PRIMITIVE;
      if (value == null) {
        valueType = ValueType.COLLECTION_PRIMITIVE;
      } else if (value.isPrimitive()) {
        valueType = value.asPrimitive().toValue() instanceof Geospatial
            ? ValueType.COLLECTION_GEOSPATIAL : ValueType.COLLECTION_PRIMITIVE;
      } else if (value.isEnum()) {
        valueType = ValueType.COLLECTION_ENUM;
      } else if (value.isLinkedComplex()) {
        valueType = ValueType.COLLECTION_LINKED_COMPLEX;
      } else if (value.isComplex()) {
        valueType = ValueType.COLLECTION_COMPLEX;
      }
      propertyResource.setValue(valueType, propertyValue);
    }
  }

  private void annotations(final ODataAnnotatable odataAnnotatable, final Annotatable annotatable) {
    for (ODataAnnotation odataAnnotation : odataAnnotatable.getAnnotations()) {
      final Annotation annotation = new AnnotationImpl();

      annotation.setTerm(odataAnnotation.getTerm());
      annotation.setType(odataAnnotation.getValue().getTypeName());
      updateValuable(annotation, odataAnnotation);

      annotatable.getAnnotations().add(annotation);
    }
  }

  @Override
  public EntitySet getEntitySet(final ODataEntitySet odataEntitySet) {
    final EntitySet entitySet = new EntitySetImpl();

    entitySet.setCount(odataEntitySet.getCount());

    final URI next = odataEntitySet.getNext();
    if (next != null) {
      entitySet.setNext(next);
    }

    for (ODataEntity entity : odataEntitySet.getEntities()) {
      entitySet.getEntities().add(getEntity(entity));
    }

    entitySet.setDeltaLink(((ODataEntitySet) odataEntitySet).getDeltaLink());
    annotations((ODataEntitySet) odataEntitySet, entitySet);
    return entitySet;
  }

  protected void links(final ODataLinked odataLinked, final Linked linked) {
    // -------------------------------------------------------------
    // Append navigation links (handling inline entity / entity set as well)
    // -------------------------------------------------------------
    // handle navigation links
    for (ODataLink link : odataLinked.getNavigationLinks()) {
      // append link
      LOG.debug("Append navigation link\n{}", link);
      linked.getNavigationLinks().add(getLink(link));
    }
    // -------------------------------------------------------------

    // -------------------------------------------------------------
    // Append association links
    // -------------------------------------------------------------
    for (ODataLink link : odataLinked.getAssociationLinks()) {
      LOG.debug("Append association link\n{}", link);
      linked.getAssociationLinks().add(getLink(link));
    }
    // -------------------------------------------------------------

    for (Link link : linked.getNavigationLinks()) {
      final ODataLink odataLink = odataLinked.getNavigationLink(link.getTitle());
      if (!(odataLink instanceof ODataInlineEntity) && !(odataLink instanceof ODataInlineEntitySet)) {
        annotations((ODataLink) odataLink, link);
      }
    }
  }

  @Override
  public Entity getEntity(final ODataEntity odataEntity) {
    final Entity entity = new EntityImpl();

    entity.setType(odataEntity.getTypeName() == null ? null : odataEntity.getTypeName().toString());

    // -------------------------------------------------------------
    // Add edit and self link
    // -------------------------------------------------------------
    final URI odataEditLink = odataEntity.getEditLink();
    if (odataEditLink != null) {
      final LinkImpl editLink = new LinkImpl();
      editLink.setTitle(entity.getType());
      editLink.setHref(odataEditLink.toASCIIString());
      editLink.setRel(Constants.EDIT_LINK_REL);
      entity.setEditLink(editLink);
    }

    if (odataEntity.isReadOnly()) {
      final LinkImpl selfLink = new LinkImpl();
      selfLink.setTitle(entity.getType());
      selfLink.setHref(odataEntity.getLink().toASCIIString());
      selfLink.setRel(Constants.SELF_LINK_REL);
      entity.setSelfLink(selfLink);
    }
    // -------------------------------------------------------------

    links(odataEntity, entity);

    // -------------------------------------------------------------
    // Append edit-media links
    // -------------------------------------------------------------
    for (ODataLink link : odataEntity.getMediaEditLinks()) {
      LOG.debug("Append edit-media link\n{}", link);
      entity.getMediaEditLinks().add(getLink(link));
    }
    // -------------------------------------------------------------

    if (odataEntity.isMediaEntity()) {
      entity.setMediaContentSource(odataEntity.getMediaContentSource());
      entity.setMediaContentType(odataEntity.getMediaContentType());
      entity.setMediaETag(odataEntity.getMediaETag());
    }

    for (ODataProperty property : odataEntity.getProperties()) {
      entity.getProperties().add(getProperty(property));
    }

    entity.setId(((ODataEntity) odataEntity).getId());
    annotations((ODataEntity) odataEntity, entity);
    return entity;
  }

  @Override
  public Link getLink(final ODataLink link) {
    final Link linkResource = new LinkImpl();
    linkResource.setRel(link.getRel());
    linkResource.setTitle(link.getName());
    linkResource.setHref(link.getLink() == null ? null : link.getLink().toASCIIString());
    linkResource.setType(link.getType().toString());
    linkResource.setMediaETag(link.getMediaETag());

    if (link instanceof ODataInlineEntity) {
      // append inline entity
      final ODataEntity inlineEntity = ((ODataInlineEntity) link).getEntity();
      LOG.debug("Append in-line entity\n{}", inlineEntity);

      linkResource.setInlineEntity(getEntity(inlineEntity));
    } else if (link instanceof ODataInlineEntitySet) {
      // append inline entity set
      final ODataEntitySet InlineEntitySet = ((ODataInlineEntitySet) link).getEntitySet();
      LOG.debug("Append in-line entity set\n{}", InlineEntitySet);

      linkResource.setInlineEntitySet(getEntitySet(InlineEntitySet));
    }

    return linkResource;
  }

  @Override
  public Property getProperty(final ODataProperty property) {
    final ODataProperty _property = (ODataProperty) property;

    final Property propertyResource = new PropertyImpl();
    propertyResource.setName(_property.getName());
    updateValuable(propertyResource, _property);
    annotations(_property, propertyResource);

    return propertyResource;
  }

  // TODO: Refactor away the v3 code
  @SuppressWarnings("unchecked")
  protected Object getValue(final ODataValue value) {
    Object valueResource;
    if (value instanceof org.apache.olingo.commons.api.domain.ODataValue
        && ((org.apache.olingo.commons.api.domain.ODataValue) value).isEnum()) {

      valueResource =
          ((org.apache.olingo.commons.api.domain.ODataValue) value).asEnum().getValue();
    } else {
      valueResource = getValueInternal(value);

      if (value instanceof org.apache.olingo.commons.api.domain.ODataValue
          && ((org.apache.olingo.commons.api.domain.ODataValue) value).isLinkedComplex()) {

        final LinkedComplexValue lcValueResource = new LinkedComplexValueImpl();
        lcValueResource.getValue().addAll((List<Property>) valueResource);

        final ODataLinkedComplexValue linked =
            ((org.apache.olingo.commons.api.domain.ODataValue) value).asLinkedComplex();
        annotations(linked, lcValueResource);
        links(linked, lcValueResource);

        valueResource = lcValueResource;
      }
    }
    return valueResource;
  }

  protected Object getValueInternal(final ODataValue value) {
    if (value == null) {
      return null;
    } else if (value.isPrimitive()) {
      return value.asPrimitive().toValue();
    } else if (value.isComplex()) {
      final ODataComplexValue<? extends ODataProperty> _value = value.asComplex();
      List<Property> valueResource = new ArrayList<Property>();

      for (final ODataProperty propertyValue : _value) {
        valueResource.add(getProperty(propertyValue));
      }
      return valueResource;
    } else if (value.isCollection()) {
      final ODataCollectionValue<? extends ODataValue> _value = value.asCollection();
      ArrayList<Object> valueResource = new ArrayList<Object>();

      for (final ODataValue collectionValue : _value) {
        valueResource.add(getValue(collectionValue));
      }
      return valueResource;
    }
    return null;
  }

  private void odataAnnotations(final Annotatable annotatable, final ODataAnnotatable odataAnnotatable) {
    for (Annotation annotation : annotatable.getAnnotations()) {
      FullQualifiedName fqn = null;
      if (client instanceof EdmEnabledODataClient) {
        final EdmTerm term = ((EdmEnabledODataClient) client).getCachedEdm().
            getTerm(new FullQualifiedName(annotation.getTerm()));
        if (term != null) {
          fqn = term.getType().getFullQualifiedName();
        }
      }

      if (fqn == null && annotation.getType() != null) {
        final EdmTypeInfo typeInfo = new EdmTypeInfo.Builder().setTypeExpression(annotation.getType()).build();
        if (typeInfo.isPrimitiveType()) {
          fqn = typeInfo.getPrimitiveTypeKind().getFullQualifiedName();
        }
      }

      final ODataAnnotation odataAnnotation = new ODataAnnotationImpl(annotation.getTerm(),
          (org.apache.olingo.commons.api.domain.ODataValue) getODataValue(fqn, annotation, null, null));
      odataAnnotatable.getAnnotations().add(odataAnnotation);
    }
  }

  @Override
  public ODataEntitySet getODataEntitySet(final ResWrap<EntitySet> resource) {
    if (LOG.isDebugEnabled()) {
      final StringWriter writer = new StringWriter();
      try {
        client.getSerializer(ODataFormat.JSON).write(writer, resource.getPayload());
      } catch (final ODataSerializerException e) {}
      writer.flush();
      LOG.debug("EntitySet -> ODataEntitySet:\n{}", writer.toString());
    }

    final URI base = resource.getContextURL() == null
        ? resource.getPayload().getBaseURI()
        : ContextURLParser.parse(resource.getContextURL()).getServiceRoot();

    final URI next = resource.getPayload().getNext();

    final ODataEntitySet entitySet = next == null
        ? client.getObjectFactory().newEntitySet()
        : client.getObjectFactory().newEntitySet(URIUtils.getURI(base, next.toASCIIString()));

    if (resource.getPayload().getCount() != null) {
      entitySet.setCount(resource.getPayload().getCount());
    }

    for (Entity entityResource : resource.getPayload().getEntities()) {
      add(entitySet, getODataEntity(
          new ResWrap<Entity>(resource.getContextURL(), resource.getMetadataETag(), entityResource)));
    }

    if (resource.getPayload().getDeltaLink() != null) {
      entitySet.setDeltaLink(URIUtils.getURI(base, resource.getPayload().getDeltaLink()));
    }
    odataAnnotations(resource.getPayload(), entitySet);

    return entitySet;
  }

  protected void odataNavigationLinks(final EdmType edmType,
      final Linked linked, final ODataLinked odataLinked, final String metadataETag, final URI base) {
    for (Link link : linked.getNavigationLinks()) {
      final String href = link.getHref();
      final String title = link.getTitle();
      final Entity inlineEntity = link.getInlineEntity();
      final EntitySet inlineEntitySet = link.getInlineEntitySet();
      if (inlineEntity == null && inlineEntitySet == null) {
        ODataLinkType linkType = null;
        if (edmType instanceof EdmStructuredType) {
          final EdmNavigationProperty navProp = ((EdmStructuredType) edmType).getNavigationProperty(title);
          if (navProp != null) {
            linkType = navProp.isCollection() ?
                ODataLinkType.ENTITY_SET_NAVIGATION :
                ODataLinkType.ENTITY_NAVIGATION;
          }
        }
        if (linkType == null) {
          linkType = link.getType() == null ?
              ODataLinkType.ENTITY_NAVIGATION :
              ODataLinkType.fromString(client.getServiceVersion(), link.getRel(), link.getType());
        }

        odataLinked.addLink(linkType == ODataLinkType.ENTITY_NAVIGATION ?
            client.getObjectFactory().newEntityNavigationLink(title, URIUtils.getURI(base, href)) :
            client.getObjectFactory().newEntitySetNavigationLink(title, URIUtils.getURI(base, href)));
      } else if (inlineEntity != null) {
        odataLinked.addLink(createODataInlineEntity(inlineEntity,
            URIUtils.getURI(base, href), title, metadataETag));
      } else {
        odataLinked.addLink(createODataInlineEntitySet(inlineEntitySet,
            URIUtils.getURI(base, href), title, metadataETag));
      }
    }

    for (org.apache.olingo.commons.api.domain.ODataLink link : odataLinked.getNavigationLinks()) {
      if (!(link instanceof ODataInlineEntity) && !(link instanceof ODataInlineEntitySet)) {
        odataAnnotations(linked.getNavigationLink(link.getName()), (ODataAnnotatable) link);
      }
    }
  }

  private ODataInlineEntity createODataInlineEntity(final Entity inlineEntity,
      final URI uri, final String title, final String metadataETag) {
    return new ODataInlineEntity(client.getServiceVersion(), uri, ODataLinkType.ENTITY_NAVIGATION, title,
        getODataEntity(new ResWrap<Entity>(
            inlineEntity.getBaseURI() == null ? null : inlineEntity.getBaseURI(), metadataETag,
            inlineEntity)));
  }

  private ODataInlineEntitySet createODataInlineEntitySet(final EntitySet inlineEntitySet,
      final URI uri, final String title, final String metadataETag) {
    return new ODataInlineEntitySet(client.getServiceVersion(), uri, ODataLinkType.ENTITY_SET_NAVIGATION, title,
        getODataEntitySet(new ResWrap<EntitySet>(
            inlineEntitySet.getBaseURI() == null ? null : inlineEntitySet.getBaseURI(), metadataETag,
            inlineEntitySet)));
  }

  private EdmEntityType findEntityType(
      final String entitySetOrSingletonOrType, final EdmEntityContainer container) {

    EdmEntityType type = null;

    final String firstToken = StringUtils.substringBefore(entitySetOrSingletonOrType, "/");
    EdmBindingTarget bindingTarget = container.getEntitySet(firstToken);
    if (bindingTarget == null) {
      bindingTarget = container.getSingleton(firstToken);
    }
    if (bindingTarget != null) {
      type = bindingTarget.getEntityType();
    }

    if (entitySetOrSingletonOrType.indexOf('/') != -1) {
      final String[] splitted = entitySetOrSingletonOrType.split("/");
      if (splitted.length > 1) {
        for (int i = 1; i < splitted.length && type != null; i++) {
          final EdmNavigationProperty navProp = type.getNavigationProperty(splitted[i]);
          if (navProp == null) {
            type = null;
          } else {
            type = navProp.getType();
          }
        }
      }
    }

    return type;
  }

  /**
   * Infer type name from various sources of information including Edm and context URL, if available.
   *
   * @param candidateTypeName type name as provided by the service
   * @param contextURL context URL
   * @param metadataETag metadata ETag
   * @return Edm type information
   */
  private EdmType findType(final String candidateTypeName, final ContextURL contextURL, final String metadataETag) {
    EdmType type = null;

    if (client instanceof EdmEnabledODataClient) {
      final Edm edm = ((EdmEnabledODataClient) client).getEdm(metadataETag);
      if (StringUtils.isNotBlank(candidateTypeName)) {
        type = edm.getEntityType(new FullQualifiedName(candidateTypeName));
      }
      if (type == null && contextURL != null) {
        if (contextURL.getDerivedEntity() == null) {
          for (EdmSchema schema : edm.getSchemas()) {
            final EdmEntityContainer container = schema.getEntityContainer();
            if (container != null) {
              final EdmEntityType entityType = findEntityType(contextURL.getEntitySetOrSingletonOrType(), container);

              if (entityType != null) {
                if (contextURL.getNavOrPropertyPath() == null) {
                  type = entityType;
                } else {
                  final EdmNavigationProperty navProp =
                      entityType.getNavigationProperty(contextURL.getNavOrPropertyPath());

                  type = navProp == null
                      ? entityType
                      : navProp.getType();
                }
              }
            }
          }
          if (type == null) {
            type = new EdmTypeInfo.Builder().setEdm(edm).
                setTypeExpression(contextURL.getEntitySetOrSingletonOrType()).build().getType();
          }
        } else {
          type = edm.getEntityType(new FullQualifiedName(contextURL.getDerivedEntity()));
        }
      }
    }

    return type;
  }

  private ODataLink createLinkFromNavigationProperty(final Property property, final String propertyTypeName) {
    if (property.isCollection()) {
      EntitySet inlineEntitySet = new EntitySetImpl();
      for (final Object inlined : property.asCollection()) {
        Entity inlineEntity = new EntityImpl();
        inlineEntity.setType(propertyTypeName);
        inlineEntity.getProperties().addAll(
            inlined instanceof LinkedComplexValue ? ((LinkedComplexValue) inlined).getValue() :
                inlined instanceof Property ? ((Property) inlined).asComplex() : null);
        inlineEntitySet.getEntities().add(inlineEntity);
      }
      return createODataInlineEntitySet(inlineEntitySet, null, property.getName(), null);
    } else {
      Entity inlineEntity = new EntityImpl();
      inlineEntity.setType(propertyTypeName);
      inlineEntity.getProperties().addAll(
          property.isLinkedComplex() ? property.asLinkedComplex().getValue() : property.asComplex());
      return createODataInlineEntity(inlineEntity, null, property.getName(), null);
    }
  }

  @Override
  public ODataEntity getODataEntity(final ResWrap<Entity> resource) {
    if (LOG.isDebugEnabled()) {
      final StringWriter writer = new StringWriter();
      try {
        client.getSerializer(ODataFormat.JSON).write(writer, resource.getPayload());
      } catch (final ODataSerializerException e) {}
      writer.flush();
      LOG.debug("EntityResource -> ODataEntity:\n{}", writer.toString());
    }

    final ContextURL contextURL = ContextURLParser.parse(resource.getContextURL());
    final URI base = resource.getContextURL() == null
        ? resource.getPayload().getBaseURI()
        : contextURL.getServiceRoot();
    final EdmType edmType = findType(resource.getPayload().getType(), contextURL, resource.getMetadataETag());
    FullQualifiedName typeName = null;
    if (resource.getPayload().getType() == null) {
      if (edmType != null) {
        typeName = edmType.getFullQualifiedName();
      }
    } else {
      typeName = new FullQualifiedName(resource.getPayload().getType());
    }

    final ODataEntity entity = resource.getPayload().getSelfLink() == null
        ? client.getObjectFactory().newEntity(typeName)
        : client.getObjectFactory().newEntity(typeName,
            URIUtils.getURI(base, resource.getPayload().getSelfLink().getHref()));

    if (StringUtils.isNotBlank(resource.getPayload().getETag())) {
      entity.setETag(resource.getPayload().getETag());
    }

    if (resource.getPayload().getEditLink() != null) {
      entity.setEditLink(URIUtils.getURI(base, resource.getPayload().getEditLink().getHref()));
    }

    for (Link link : resource.getPayload().getAssociationLinks()) {
      entity.addLink(client.getObjectFactory().
          newAssociationLink(link.getTitle(), URIUtils.getURI(base, link.getHref())));
    }

    odataNavigationLinks(edmType, resource.getPayload(), entity, resource.getMetadataETag(), base);

    for (Link link : resource.getPayload().getMediaEditLinks()) {
      entity.addLink(client.getObjectFactory().
          newMediaEditLink(link.getTitle(), URIUtils.getURI(base, link.getHref())));
    }

    for (ODataOperation operation : resource.getPayload().getOperations()) {
      operation.setTarget(URIUtils.getURI(base, operation.getTarget()));
      entity.getOperations().add(operation);
    }

    if (resource.getPayload().isMediaEntity()) {
      entity.setMediaEntity(true);
      entity.setMediaContentSource(URIUtils.getURI(base, resource.getPayload().getMediaContentSource()));
      entity.setMediaContentType(resource.getPayload().getMediaContentType());
      entity.setMediaETag(resource.getPayload().getMediaETag());
    }

    for (final Property property : resource.getPayload().getProperties()) {
      EdmType propertyType = null;
      if (edmType instanceof EdmEntityType) {
        final EdmElement edmProperty = ((EdmEntityType) edmType).getProperty(property.getName());
        if (edmProperty != null) {
          propertyType = edmProperty.getType();
          if (edmProperty instanceof EdmNavigationProperty) {
            final String propertyTypeName = propertyType.getFullQualifiedName().getFullQualifiedNameAsString();
            entity.addLink(createLinkFromNavigationProperty(property, propertyTypeName));
            break;
          }
        }
      }
      add(entity, getODataProperty(propertyType, property));
    }

    entity.setId(resource.getPayload().getId());
    odataAnnotations(resource.getPayload(), entity);

    return entity;
  }

  @Override
  public ODataProperty getODataProperty(final ResWrap<Property> resource) {
    final Property payload = resource.getPayload();
    final EdmTypeInfo typeInfo = buildTypeInfo(ContextURLParser.parse(resource.getContextURL()),
        resource.getMetadataETag(), payload.getName(), payload.getType());

    final ODataProperty property = new ODataPropertyImpl(payload.getName(),
        getODataValue(typeInfo == null ? null : typeInfo.getFullQualifiedName(),
            payload, resource.getContextURL(), resource.getMetadataETag()));
    odataAnnotations(payload, property);

    return property;
  }

  private EdmTypeInfo buildTypeInfo(final ContextURL contextURL, final String metadataETag,
      final String propertyName, final String propertyType) {

    FullQualifiedName typeName = null;
    final EdmType type = findType(null, contextURL, metadataETag);
    if (type instanceof EdmStructuredType) {
      final EdmProperty edmProperty = ((EdmStructuredType) type).getStructuralProperty(propertyName);
      if (edmProperty != null) {
        typeName = edmProperty.getType().getFullQualifiedName();
      }
    }
    if (typeName == null && type != null) {
      typeName = type.getFullQualifiedName();
    }

    return buildTypeInfo(typeName, propertyType);
  }

  private EdmTypeInfo buildTypeInfo(final FullQualifiedName typeName, final String propertyType) {
    EdmTypeInfo typeInfo = null;
    if (typeName == null) {
      if (propertyType != null) {
        typeInfo = new EdmTypeInfo.Builder().setTypeExpression(propertyType).build();
      }
    } else {
      if (propertyType == null || propertyType.equals(EdmPrimitiveTypeKind.String.getFullQualifiedName().toString())) {
        typeInfo = new EdmTypeInfo.Builder().setTypeExpression(typeName.toString()).build();
      } else {
        typeInfo = new EdmTypeInfo.Builder().setTypeExpression(propertyType).build();
      }
    }
    return typeInfo;
  }

  protected ODataProperty getODataProperty(final EdmType type, final Property resource) {
    final EdmTypeInfo typeInfo = buildTypeInfo(type == null ? null : type.getFullQualifiedName(), resource.getType());

    final ODataProperty property = new ODataPropertyImpl(resource.getName(),
        getODataValue(typeInfo == null ? null : typeInfo.getFullQualifiedName(),
            resource, null, null));
    odataAnnotations(resource, property);

    return property;
  }

  protected ODataValue getODataValue(final FullQualifiedName type,
      final Valuable valuable, final URI contextURL, final String metadataETag) {

    // fixes enum values treated as primitive when no type information is available
    if (client instanceof EdmEnabledODataClient && type != null) {
      final EdmEnumType edmType = ((EdmEnabledODataClient) client).getEdm(metadataETag).getEnumType(type);
      if (valuable.isPrimitive() && edmType != null) {
        valuable.setValue(ValueType.ENUM, valuable.asPrimitive());
      }
    }

    ODataValue value = null;
    if (valuable.isEnum()) {
      value = ((ODataClient) client).getObjectFactory().newEnumValue(type == null ? null : type.toString(),
          valuable.asEnum().toString());
    } else if (valuable.isLinkedComplex()) {
      final ODataLinkedComplexValue lcValue =
          ((ODataClient) client).getObjectFactory().newLinkedComplexValue(type == null ? null : type.toString());

      EdmComplexType edmType = null;
      if (client instanceof EdmEnabledODataClient && type != null) {
        edmType = ((EdmEnabledODataClient) client).getEdm(metadataETag).getComplexType(type);
      }

      for (Property property : valuable.asLinkedComplex().getValue()) {
        EdmType edmPropertyType = null;
        if (edmType != null) {
          final EdmElement edmProp = edmType.getProperty(property.getName());
          if (edmProp != null) {
            edmPropertyType = edmProp.getType();
          }
        }
        lcValue.add(getODataProperty(edmPropertyType, property));
      }

      odataNavigationLinks(edmType, valuable.asLinkedComplex(), lcValue, metadataETag, contextURL);
      odataAnnotations(valuable.asLinkedComplex(), lcValue);

      value = lcValue;
    } else {
      if (valuable.isGeospatial()) {
        value = client.getObjectFactory().newPrimitiveValueBuilder().
            setValue(valuable.asGeospatial()).
            setType(type == null
                || EdmPrimitiveTypeKind.Geography.getFullQualifiedName().equals(type)
                || EdmPrimitiveTypeKind.Geometry.getFullQualifiedName().equals(type)
                ? valuable.asGeospatial().getEdmPrimitiveTypeKind()
                : EdmPrimitiveTypeKind.valueOfFQN(client.getServiceVersion(), type.toString())).
            build();
      } else if (valuable.isPrimitive() || valuable.getValueType() == null) {
        // fixes non-string values treated as string when no type information is available at de-serialization level
        if (type != null && !EdmPrimitiveTypeKind.String.getFullQualifiedName().equals(type)
            && EdmPrimitiveType.EDM_NAMESPACE.equals(type.getNamespace())
            && valuable.asPrimitive() instanceof String) {

          final EdmPrimitiveType primitiveType =
              EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.valueOf(type.getName()));
          final Class<?> returnType = primitiveType.getDefaultType().isAssignableFrom(Calendar.class)
              ? Timestamp.class : primitiveType.getDefaultType();
          try {
            valuable.setValue(valuable.getValueType(),
                primitiveType.valueOfString(valuable.asPrimitive().toString(),
                    null, null, Constants.DEFAULT_PRECISION, Constants.DEFAULT_SCALE, null,
                    returnType));
          } catch (EdmPrimitiveTypeException e) {
            throw new IllegalArgumentException(e);
          }
        }

        value = client.getObjectFactory().newPrimitiveValueBuilder().
            setValue(valuable.asPrimitive()).
            setType(type == null || !EdmPrimitiveType.EDM_NAMESPACE.equals(type.getNamespace())
                ? null
                : EdmPrimitiveTypeKind.valueOfFQN(client.getServiceVersion(), type.toString())).
            build();
      } else if (valuable.isComplex()) {
        final ODataComplexValue<ODataProperty> cValue =
            (ODataComplexValue<ODataProperty>) client.getObjectFactory().
                newComplexValue(type == null ? null : type.toString());

        if (!valuable.isNull()) {
          EdmComplexType edmType = null;
          if (client instanceof EdmEnabledODataClient && type != null) {
            edmType = ((EdmEnabledODataClient) client).getEdm(metadataETag).getComplexType(type);
          }

          for (Property property : valuable.asComplex()) {
            EdmType edmPropertyType = null;
            if (edmType != null) {
              final EdmElement edmProp = edmType.getProperty(property.getName());
              if (edmProp != null) {
                edmPropertyType = edmProp.getType();
              }
            }

            cValue.add(getODataProperty(edmPropertyType, property));
          }
        }

        value = cValue;
      } else if (valuable.isCollection()) {
        value =
            client.getObjectFactory().newCollectionValue(type == null ? null : "Collection(" + type.toString() + ")");

        for (Object _value : valuable.asCollection()) {
          final Property fake = new PropertyImpl();
          fake.setValue(valuable.getValueType().getBaseType(), _value);
          value.asCollection().add(getODataValue(type, fake, contextURL, metadataETag));
        }
      }
    }

    return value;
  }

  @Override
  public ODataDelta getODataDelta(final ResWrap<Delta> resource) {
    final URI base = resource.getContextURL() == null
        ? resource.getPayload().getBaseURI()
        : ContextURLParser.parse(resource.getContextURL()).getServiceRoot();

    final URI next = resource.getPayload().getNext();

    final ODataDelta delta = next == null
        ? ((ODataClient) client).getObjectFactory().newDelta()
        : ((ODataClient) client).getObjectFactory().newDelta(URIUtils.getURI(base, next.toASCIIString()));

    if (resource.getPayload().getCount() != null) {
      delta.setCount(resource.getPayload().getCount());
    }

    if (resource.getPayload().getDeltaLink() != null) {
      delta.setDeltaLink(URIUtils.getURI(base, resource.getPayload().getDeltaLink()));
    }

    for (Entity entityResource : resource.getPayload().getEntities()) {
      add(delta, getODataEntity(
          new ResWrap<Entity>(resource.getContextURL(), resource.getMetadataETag(), entityResource)));
    }
    for (DeletedEntity deletedEntity : resource.getPayload().getDeletedEntities()) {
      final ODataDeletedEntityImpl impl = new ODataDeletedEntityImpl();
      impl.setId(URIUtils.getURI(base, deletedEntity.getId()));
      impl.setReason(Reason.valueOf(deletedEntity.getReason().name()));

      delta.getDeletedEntities().add(impl);
    }

    odataAnnotations(resource.getPayload(), delta);

    for (DeltaLink link : resource.getPayload().getAddedLinks()) {
      final ODataDeltaLink impl = new ODataDeltaLinkImpl();
      impl.setRelationship(link.getRelationship());
      impl.setSource(URIUtils.getURI(base, link.getSource()));
      impl.setTarget(URIUtils.getURI(base, link.getTarget()));

      odataAnnotations(link, impl);

      delta.getAddedLinks().add(impl);
    }
    for (DeltaLink link : resource.getPayload().getDeletedLinks()) {
      final ODataDeltaLink impl = new ODataDeltaLinkImpl();
      impl.setRelationship(link.getRelationship());
      impl.setSource(URIUtils.getURI(base, link.getSource()));
      impl.setTarget(URIUtils.getURI(base, link.getTarget()));

      odataAnnotations(link, impl);

      delta.getDeletedLinks().add(impl);
    }

    return delta;
  }
}
