/**
This code was originally automatically generated from the UML model in
https://github.com/sciserver/resource-management/blob/master/vo-urp/RACM_v1.xml
using the VO-URP tool, https://github.com/sciserver/vo-urp. 
It is now included in the code-base and will no longer be 
generated automatically. You can edit this file, but be aware
of its origins when interpreting it.
**/

package edu.jhu.file;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import org.ivoa.dm.model.MetadataElement;
import org.ivoa.dm.model.IMetadataObjectContainer;
import org.ivoa.dm.model.Reference;
import org.ivoa.dm.model.TransientObjectManager;
import org.ivoa.dm.model.MetadataObject;
import org.ivoa.dm.model.MetadataRootEntityObject;
import edu.jhu.rac.ResourceContext;

import static edu.jhu.ModelVersion.LAST_MODIFICATION_DATE;

/**
 * UML Object FileService :
 *
 * 
 * TODO : Missing description : please, update your UML model asap.
 *
 * 
 * @author generated by VO-URP tools VO-URP Home
 * @author Laurent Bourges (voparis) / Gerard Lemson (mpe)
 */

@javax.persistence.Entity @javax.persistence.Table(name = "t_FileService")

@javax.persistence.Inheritance(strategy = javax.persistence.InheritanceType.JOINED) @javax.persistence.DiscriminatorColumn(name = "DTYPE", discriminatorType = javax.persistence.DiscriminatorType.STRING, length = 32)

@javax.persistence.NamedQueries({
        @javax.persistence.NamedQuery(name = "FileService.findById", query = "SELECT o FROM FileService o WHERE o.id = :id"),
        @javax.persistence.NamedQuery(name = "FileService.findByPublisherDID", query = "SELECT o FROM FileService o WHERE o.identity.publisherDID = :publisherDID")

        ,
        @javax.persistence.NamedQuery(name = "FileService.findByName", query = "SELECT o FROM FileService o WHERE o.name = :name")

})

public class FileService extends MetadataRootEntityObject {

    /** serial uid = last modification date of the UML model. */
    private static final long serialVersionUID = LAST_MODIFICATION_DATE;

    /** jpaVersion gives the current version number for that entity (used by pessimistic / optimistic locking in JPA) */
    @javax.persistence.Version() @javax.persistence.Column(name = "OPTLOCK")
    protected int jpaVersion;

    /**
     * Attribute name :
     * 
     * TODO : Missing description : please, update your UML model asap.
     * 
     * ( Multiplicity : 1 )
     */

    @javax.persistence.Basic(optional = false) @javax.persistence.Column(name = "name", nullable = false)

    private String name;

    /**
     * Attribute description :
     * 
     * TODO : Missing description : please, update your UML model asap.
     * 
     * ( Multiplicity : 0..1 , MaxLength : -1 )
     */

    @javax.persistence.Basic(fetch = javax.persistence.FetchType.EAGER, optional = true) @javax.persistence.Lob @javax.persistence.Column(name = "description", nullable = true)

    private String description;

    /**
     * Attribute apiEndpoint : The base URL of the FileServices API. ( Multiplicity : 1 )
     */

    @javax.persistence.Basic(optional = false) @javax.persistence.Column(name = "apiEndpoint", nullable = false)

    private String apiEndpoint;

    /**
     * Attribute serviceToken :
     * 
     * TODO : Missing description : please, update your UML model asap.
     * 
     * ( Multiplicity : 1 )
     */

    @javax.persistence.Basic(optional = false) @javax.persistence.Column(name = "serviceToken", nullable = false)

    private String serviceToken;

    /**
     * Collection rootVolume :
     * 
     * TODO : Missing description : please, update your UML model asap.
     * 
     * ( Multiplicity : 1..* )
     */

    @javax.persistence.OneToMany(cascade = javax.persistence.CascadeType.ALL, fetch = javax.persistence.FetchType.LAZY, mappedBy = "container", orphanRemoval = true)

    private List<RootVolume> rootVolume = null;

    /**
     * Collection userVolumes :
     * 
     * TODO : Missing description : please, update your UML model asap.
     * 
     * ( Multiplicity : 0..* )
     */

    @javax.persistence.OneToMany(cascade = javax.persistence.CascadeType.ALL, fetch = javax.persistence.FetchType.LAZY, mappedBy = "container", orphanRemoval = true)

    private List<UserVolume> userVolumes = null;

    /**
     * Reference resourceContext :
     * 
     * TODO : Missing description : please, update your UML model asap.
     * 
     * ( Multiplicity : 1 )
     */

    @javax.persistence.ManyToOne(optional = false, fetch = javax.persistence.FetchType.LAZY, cascade = {
            javax.persistence.CascadeType.PERSIST, javax.persistence.CascadeType.MERGE,
            javax.persistence.CascadeType.REFRESH }) @javax.persistence.JoinColumn(name = "resourceContextId", referencedColumnName = "id", nullable = false)

    private ResourceContext resourceContext = null;
    /**
     * "lazy" version of the resourceContext reference. Used by XML (un)marshallers to resolve possibly inderectly
     * referenced resource ResourceContext.
     */

    @javax.persistence.Transient

    protected Reference p_resourceContext = null;

    /**
     * Creates a new FileService.
     */
    protected FileService() {
        super();
    }

    /**
     * Creates a new FileService for the given Container Entity.
     *
     * The Parent Container CAN NOT BE NULL
     *
     * @param pContainer the parent container CAN NOT BE NULL
     */
    public FileService(final TransientObjectManager pContainer) {
        super(pContainer);
    }

    /**
     * Returns name Attribute
     * 
     * @return name Attribute
     */
    public String getName() {
        return this.name;
    }

    /**
     * Defines name Attribute
     * 
     * @param pName value to set
     */
    public void setName(final String pName) {
        this.name = pName;
    }

    /**
     * Returns description Attribute
     * 
     * @return description Attribute
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Defines description Attribute
     * 
     * @param pDescription value to set
     */
    public void setDescription(final String pDescription) {
        this.description = pDescription;
    }

    /**
     * Returns apiEndpoint Attribute
     * 
     * @return apiEndpoint Attribute
     */
    public String getApiEndpoint() {
        return this.apiEndpoint;
    }

    /**
     * Defines apiEndpoint Attribute
     * 
     * @param pApiEndpoint value to set
     */
    public void setApiEndpoint(final String pApiEndpoint) {
        this.apiEndpoint = pApiEndpoint;
    }

    /**
     * Returns serviceToken Attribute
     * 
     * @return serviceToken Attribute
     */
    public String getServiceToken() {
        return this.serviceToken;
    }

    /**
     * Defines serviceToken Attribute
     * 
     * @param pServiceToken value to set
     */
    public void setServiceToken(final String pServiceToken) {
        this.serviceToken = pServiceToken;
    }

    /**
     * Returns rootVolume Collection
     * 
     * @return rootVolume Collection
     */
    public List<RootVolume> getRootVolume() {
        return this.rootVolume;
    }

    /**
     * Defines rootVolume Collection
     * 
     * @param pRootVolume collection to set
     */
    public void setRootVolume(final List<RootVolume> pRootVolume) {
        this.rootVolume = pRootVolume;
    }

    /**
     * Add a RootVolume to the collection
     * 
     * @param pRootVolume RootVolume to add
     */
    public void addRootVolume(final RootVolume pRootVolume) {
        if (this.rootVolume == null) {
            this.rootVolume = new ArrayList<RootVolume>();
        }

        this.rootVolume.add(pRootVolume);
    }

    /**
     * Returns userVolumes Collection
     * 
     * @return userVolumes Collection
     */
    public List<UserVolume> getUserVolumes() {
        return this.userVolumes;
    }

    /**
     * Defines userVolumes Collection
     * 
     * @param pUserVolumes collection to set
     */
    public void setUserVolumes(final List<UserVolume> pUserVolumes) {
        this.userVolumes = pUserVolumes;
    }

    /**
     * Add a UserVolume to the collection
     * 
     * @param pUserVolume UserVolume to add
     */
    public void addUserVolumes(final UserVolume pUserVolume) {
        if (this.userVolumes == null) {
            this.userVolumes = new ArrayList<UserVolume>();
        }

        this.userVolumes.add(pUserVolume);
    }

    /**
     * Returns resourceContext Reference If the resourceContext variable is null but its "lazy" version
     * p_resourceContext is not, that lazy reference will be resolved to the actual object.
     * 
     * @return resourceContext Reference
     */
    public ResourceContext getResourceContext() {

        if (this.resourceContext == null && this.p_resourceContext != null) {
            this.resourceContext = (ResourceContext) resolve(this.p_resourceContext, ResourceContext.class);
        }
        return this.resourceContext;

    }

    /**
     * Defines resourceContext Reference
     * 
     * @param pResourceContext reference to set
     */
    public void setResourceContext(final ResourceContext pResourceContext) {

        this.resourceContext = pResourceContext;

    }

    /**
     * Returns Jpa version for optimistic locking.
     * 
     * @return jpa version number
     */
    protected int getJpaVersion() {
        return this.jpaVersion;
    }

    /**
     * Returns equals from id attribute here. Child classes can override this method to allow deep equals with
     * attributes / references / collections
     *
     * @param object the reference object with which to compare.
     * @param isDeep true means to call hashCode(sb, true) for all attributes / references / collections which are
     * MetadataElement implementations
     *
     * @return <code>true</code> if this object is the same as the obj argument; <code>false</code> otherwise.
     */
    @Override
    public boolean equals(final Object object, final boolean isDeep) {
        /* identity, nullable, class and identifiers checks */
        if (!(super.equals(object, isDeep))) {
            return false;
        }

        /* do check values (attributes / references / collections) */

        if (isDeep) {

            final FileService other = (FileService) object;

            if (!areEquals(this.name, other.name)) {
                return false;
            }

            if (!areEquals(this.description, other.description)) {
                return false;
            }

            if (!areEquals(this.apiEndpoint, other.apiEndpoint)) {
                return false;
            }

            if (!areEquals(this.serviceToken, other.serviceToken)) {
                return false;
            }

        }

        return true;
    }

    /**
     * Returns the property value given the property name. Can be any property (internal, attribute, reference,
     * collection) and all type must be supported (dataType, objectType, enumeration)
     *
     * @param propertyName name of the property (like in UML model)
     *
     * @return property value or null if unknown or not defined
     */
    @Override
    public Object getProperty(final String propertyName) {
        // first : checks if propertyName is null or empty :
        if (propertyName == null) {
            return null;
        }
        // second : search in parent classes (maybe null) :
        Object res = super.getProperty(propertyName);

        if ("name".equals(propertyName)) {
            return getName();
        }

        if ("description".equals(propertyName)) {
            return getDescription();
        }

        if ("apiEndpoint".equals(propertyName)) {
            return getApiEndpoint();
        }

        if ("serviceToken".equals(propertyName)) {
            return getServiceToken();
        }

        if ("rootVolume".equals(propertyName)) {
            return getRootVolume();
        }

        if ("userVolumes".equals(propertyName)) {
            return getUserVolumes();
        }

        if ("resourceContext".equals(propertyName)) {
            return getResourceContext();
        }

        return res;
    }

    /**
     * Sets the property value to the given property name. Can be any property (internal, attribute, reference,
     * collection) and all type must be supported (dataType, objectType, enumeration)
     *
     * @param propertyName name of the property (like in UML model)
     *
     * @param pValue to be set
     * 
     * @return true if property has been set
     */
    @Override
    public boolean setProperty(final String propertyName, final Object pValue) {
        // first : checks if propertyName is null or empty :
        if (propertyName == null) {
            return false;
        }
        // second : search in parent classes (maybe null) :
        boolean res = super.setProperty(propertyName, pValue);

        if (!res) {

            if ("name".equals(propertyName)) {
                setName((String) pValue);
                return true;
            }

            if ("description".equals(propertyName)) {
                setDescription((String) pValue);
                return true;
            }

            if ("apiEndpoint".equals(propertyName)) {
                setApiEndpoint((String) pValue);
                return true;
            }

            if ("serviceToken".equals(propertyName)) {
                setServiceToken((String) pValue);
                return true;
            }

            if ("resourceContext".equals(propertyName)) {
                setResourceContext((ResourceContext) pValue);
                return true;
            }

        }

        return res;
    }

    /**
     * Sets all Reference fields to their appropriate value.<br/>
     */
    @Override
    protected void prepareReferencesForMarshalling() {
        super.prepareReferencesForMarshalling();

        if (getResourceContext() != null) {
            this.p_resourceContext = getResourceContext().asReference();
            if (getStateFor(getResourceContext()).isToBeMarshalled()) {
                getResourceContext().setXmlId();
            }
        }

    }

    /**
     * Resets all Reference fields to null.<br/>
     */
    @Override
    protected void resetReferencesAfterMarshalling() {
        super.prepareReferencesForMarshalling();

        this.p_resourceContext = null;

    }

    /**
     * Puts the string representation in the given string buffer : <br>
     * "Type =[class name @ hashcode] : { field name = field value , ...}"
     *
     * @param sb given string buffer to fill
     * @param isDeep true means to call deepToString(sb, true, ids) for all attributes / references / collections which
     * are MetadataElement implementations
     * @param ids identity map to avoid cyclic loops
     *
     * @return stringbuffer the given string buffer filled with the string representation
     */
    @Override
    protected StringBuilder deepToString(final StringBuilder sb, final boolean isDeep,
            final Map<MetadataElement, Object> ids) {

        sb.append("\n[ FileService");
        sb.append("={");

        sb.append("name=");
        if (getName() != null) {
            MetadataElement.deepToString(sb, isDeep, ids, getName());
        }
        sb.append(", ");
        sb.append("description=");
        if (getDescription() != null) {
            MetadataElement.deepToString(sb, isDeep, ids, getDescription());
        }
        sb.append(", ");
        sb.append("apiEndpoint=");
        if (getApiEndpoint() != null) {
            MetadataElement.deepToString(sb, isDeep, ids, getApiEndpoint());
        }
        sb.append(", ");
        sb.append("serviceToken=");
        if (getServiceToken() != null) {
            MetadataElement.deepToString(sb, isDeep, ids, getServiceToken());
        }
        sb.append(", ");
        sb.append("rootVolume=");
        if (getRootVolume() != null) {
            MetadataElement.deepToString(sb, isDeep, ids, getRootVolume());
        }
        sb.append(", ");
        sb.append("userVolumes=");
        if (getUserVolumes() != null) {
            MetadataElement.deepToString(sb, isDeep, ids, getUserVolumes());
        }
        sb.append(", ");
        sb.append("resourceContext.id=");
        if (getResourceContext() != null) {
            getResourceContext().getId();
        }

        return sb.append("} ]");

    }

    public boolean isValid() {
        if (!super.isValid())
            return false;
        boolean isOk = true;
        isOk = isOk && name != null;
        isOk = isOk && apiEndpoint != null;
        isOk = isOk && serviceToken != null;
        isOk = isOk && (resourceContext != null || p_resourceContext != null);
        return isOk;
    }

    public String validationErrors() {
        StringBuffer sb = new StringBuffer(super.validationErrors());

        if (name == null)
            sb.append("- atribute 'name' cannot be null\n");

        if (apiEndpoint == null)
            sb.append("- atribute 'apiEndpoint' cannot be null\n");

        if (serviceToken == null)
            sb.append("- atribute 'serviceToken' cannot be null\n");

        if (resourceContext == null && p_resourceContext == null)
            sb.append("- reference 'resourceContext' cannot be null\n");

        return sb.toString();
    }

}
