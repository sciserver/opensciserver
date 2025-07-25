/**
This code was originally automatically generated from the UML model in
https://github.com/sciserver/resource-management/blob/master/vo-urp/RACM_v1.xml
using the VO-URP tool, https://github.com/sciserver/vo-urp. 
It is now included in the code-base and will no longer be 
generated automatically. You can edit this file, but be aware
of its origins when interpreting it.
**/

package edu.jhu.job;

import java.util.Date;

import java.util.Map;
import org.ivoa.dm.model.MetadataElement;
import org.ivoa.dm.model.IMetadataObjectContainer;
import org.ivoa.dm.model.Reference;
import org.ivoa.dm.model.TransientObjectManager;
import org.ivoa.dm.model.MetadataObject;
import org.ivoa.dm.model.IMetadataObjectContainer;

import edu.jhu.rac.Action;

import edu.jhu.rac.Resource;

import edu.jhu.user.User;

import static edu.jhu.ModelVersion.LAST_MODIFICATION_DATE;

/**
 * UML Object ActionExecution :
 *
 * 
 * TODO : Missing description : please, update your UML model asap.
 *
 * 
 * @author generated by VO-URP tools VO-URP Home
 * @author Laurent Bourges (voparis) / Gerard Lemson (mpe)
 */

@javax.persistence.Entity @javax.persistence.Table(name = "t_ActionExecution")

@javax.persistence.Inheritance(strategy = javax.persistence.InheritanceType.JOINED) @javax.persistence.DiscriminatorColumn(name = "DTYPE", discriminatorType = javax.persistence.DiscriminatorType.STRING, length = 32)

@javax.persistence.NamedQueries({
        @javax.persistence.NamedQuery(name = "ActionExecution.findById", query = "SELECT o FROM ActionExecution o WHERE o.id = :id"),
        @javax.persistence.NamedQuery(name = "ActionExecution.findByPublisherDID", query = "SELECT o FROM ActionExecution o WHERE o.identity.publisherDID = :publisherDID")

})

public class ActionExecution extends MetadataObject {

    /** serial uid = last modification date of the UML model. */
    private static final long serialVersionUID = LAST_MODIFICATION_DATE;

    /** jpaVersion gives the current version number for that entity (used by pessimistic / optimistic locking in JPA) */
    @javax.persistence.Version() @javax.persistence.Column(name = "OPTLOCK")
    protected int jpaVersion;

    /** container gives the parent entity which owns a collection containing instances of this class */
    @javax.persistence.ManyToOne(cascade = { javax.persistence.CascadeType.PERSIST, javax.persistence.CascadeType.MERGE,
            javax.persistence.CascadeType.REFRESH }, fetch = javax.persistence.FetchType.LAZY) @javax.persistence.JoinColumn(name = "containerId", referencedColumnName = "id", nullable = false)
    protected History container;

    /**
     * Attribute executionDate :
     * 
     * TODO : Missing description : please, update your UML model asap.
     * 
     * ( Multiplicity : 1 )
     */

    @javax.persistence.Basic(optional = false) @javax.persistence.Temporal(javax.persistence.TemporalType.TIMESTAMP) @javax.persistence.Column(name = "executionDate", nullable = false)

    private Date executionDate;

    /**
     * Attribute status :
     * 
     * TODO : Missing description : please, update your UML model asap.
     * 
     * ( Multiplicity : 1 )
     */

    @javax.persistence.Basic(optional = false) @javax.persistence.Column(name = "status", nullable = false)

    private String status;

    /**
     * Reference action :
     * 
     * TODO : Missing description : please, update your UML model asap.
     * 
     * ( Multiplicity : 1 )
     */

    @javax.persistence.ManyToOne(optional = false, fetch = javax.persistence.FetchType.LAZY, cascade = {
            javax.persistence.CascadeType.PERSIST, javax.persistence.CascadeType.MERGE,
            javax.persistence.CascadeType.REFRESH }) @javax.persistence.JoinColumn(name = "actionId", referencedColumnName = "id", nullable = false)

    private Action action = null;
    /**
     * "lazy" version of the action reference. Used by XML (un)marshallers to resolve possibly inderectly referenced
     * resource Action.
     */

    @javax.persistence.Transient

    protected Reference p_action = null;

    /**
     * Reference user :
     * 
     * TODO : Missing description : please, update your UML model asap.
     * 
     * ( Multiplicity : 1 )
     */

    @javax.persistence.ManyToOne(optional = false, fetch = javax.persistence.FetchType.LAZY, cascade = {
            javax.persistence.CascadeType.PERSIST, javax.persistence.CascadeType.MERGE,
            javax.persistence.CascadeType.REFRESH }) @javax.persistence.JoinColumn(name = "userId", referencedColumnName = "id", nullable = false)

    private User user = null;
    /**
     * "lazy" version of the user reference. Used by XML (un)marshallers to resolve possibly inderectly referenced
     * resource User.
     */

    @javax.persistence.Transient

    protected Reference p_user = null;

    /**
     * Reference resource :
     * 
     * TODO : Missing description : please, update your UML model asap.
     * 
     * ( Multiplicity : 1 )
     */

    @javax.persistence.ManyToOne(optional = false, fetch = javax.persistence.FetchType.LAZY, cascade = {
            javax.persistence.CascadeType.PERSIST, javax.persistence.CascadeType.MERGE,
            javax.persistence.CascadeType.REFRESH }) @javax.persistence.JoinColumn(name = "resourceId", referencedColumnName = "id", nullable = false)

    private Resource resource = null;
    /**
     * "lazy" version of the resource reference. Used by XML (un)marshallers to resolve possibly inderectly referenced
     * resource Resource.
     */

    @javax.persistence.Transient

    protected Reference p_resource = null;

    /**
     * Creates a new ActionExecution.
     */
    protected ActionExecution() {
        super();
    }

    /**
     * Creates a new ActionExecution for the given Container Entity.
     *
     * The Parent Container CAN NOT BE NULL
     *
     * @param pContainer the parent container CAN NOT BE NULL
     */
    public ActionExecution(final History pContainer) {
        super();
        this.setContainer(pContainer);
    }

    /**
     * Returns executionDate Attribute
     * 
     * @return executionDate Attribute
     */
    public Date getExecutionDate() {
        return this.executionDate;
    }

    /**
     * Defines executionDate Attribute
     * 
     * @param pExecutionDate value to set
     */
    public void setExecutionDate(final Date pExecutionDate) {
        this.executionDate = pExecutionDate;
    }

    /**
     * Returns status Attribute
     * 
     * @return status Attribute
     */
    public String getStatus() {
        return this.status;
    }

    /**
     * Defines status Attribute
     * 
     * @param pStatus value to set
     */
    public void setStatus(final String pStatus) {
        this.status = pStatus;
    }

    /**
     * Returns action Reference If the action variable is null but its "lazy" version p_action is not, that lazy
     * reference will be resolved to the actual object.
     * 
     * @return action Reference
     */
    public Action getAction() {

        if (this.action == null && this.p_action != null) {
            this.action = (Action) resolve(this.p_action, Action.class);
        }
        return this.action;

    }

    /**
     * Defines action Reference
     * 
     * @param pAction reference to set
     */
    public void setAction(final Action pAction) {

        this.action = pAction;

    }

    /**
     * Returns user Reference If the user variable is null but its "lazy" version p_user is not, that lazy reference
     * will be resolved to the actual object.
     * 
     * @return user Reference
     */
    public User getUser() {

        if (this.user == null && this.p_user != null) {
            this.user = (User) resolve(this.p_user, User.class);
        }
        return this.user;

    }

    /**
     * Defines user Reference
     * 
     * @param pUser reference to set
     */
    public void setUser(final User pUser) {

        this.user = pUser;

    }

    /**
     * Returns resource Reference If the resource variable is null but its "lazy" version p_resource is not, that lazy
     * reference will be resolved to the actual object.
     * 
     * @return resource Reference
     */
    public Resource getResource() {

        if (this.resource == null && this.p_resource != null) {
            this.resource = (Resource) resolve(this.p_resource, Resource.class);
        }
        return this.resource;

    }

    /**
     * Defines resource Reference
     * 
     * @param pResource reference to set
     */
    public void setResource(final Resource pResource) {

        this.resource = pResource;

    }

    /**
     * Returns the Container Entity == 'Parent'.
     * 
     * @return the parent container Entity
     */
    public History getContainer() {
        return this.container;
    }

    @Override
    public IMetadataObjectContainer container() {
        return getContainer();
    }

    /**
     * Sets the Container Entity == 'Parent' ONLY.
     * 
     * @param pContainer the parent container
     */
    private void setContainerField(final History pContainer) {
        this.container = pContainer;
    }

    /**
     * Sets the Container Entity == 'Parent' and adds this to the appropriate collection on the container.
     *
     * @param pContainer the parent container CAN NOT BE NULL
     *
     * @throws IllegalStateException if pContainer is null !
     */
    protected void setContainer(final History pContainer) {
        if (pContainer == null) {
            throw new IllegalStateException("The parent container can not be null !");
        }
        setContainerField(pContainer);
        pContainer.addActions(this);
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

            final ActionExecution other = (ActionExecution) object;

            if (!areEquals(this.executionDate, other.executionDate)) {
                return false;
            }

            if (!areEquals(this.status, other.status)) {
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

        if (PROPERTY_CONTAINER.equals(propertyName)) {
            return getContainer();
        }

        if ("executionDate".equals(propertyName)) {
            return getExecutionDate();
        }

        if ("status".equals(propertyName)) {
            return getStatus();
        }

        if ("action".equals(propertyName)) {
            return getAction();
        }

        if ("user".equals(propertyName)) {
            return getUser();
        }

        if ("resource".equals(propertyName)) {
            return getResource();
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

            if (PROPERTY_CONTAINER.equals(propertyName)) {
                setContainerField((History) pValue);
                return true;
            }

            if ("executionDate".equals(propertyName)) {
                setExecutionDate((Date) pValue);
                return true;
            }

            if ("status".equals(propertyName)) {
                setStatus((String) pValue);
                return true;
            }

            if ("action".equals(propertyName)) {
                setAction((Action) pValue);
                return true;
            }

            if ("user".equals(propertyName)) {
                setUser((User) pValue);
                return true;
            }

            if ("resource".equals(propertyName)) {
                setResource((Resource) pValue);
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

        if (getAction() != null) {
            this.p_action = getAction().asReference();
            if (getStateFor(getAction()).isToBeMarshalled()) {
                getAction().setXmlId();
            }
        }

        if (getUser() != null) {
            this.p_user = getUser().asReference();
            if (getStateFor(getUser()).isToBeMarshalled()) {
                getUser().setXmlId();
            }
        }

        if (getResource() != null) {
            this.p_resource = getResource().asReference();
            if (getStateFor(getResource()).isToBeMarshalled()) {
                getResource().setXmlId();
            }
        }

    }

    /**
     * Resets all Reference fields to null.<br/>
     */
    @Override
    protected void resetReferencesAfterMarshalling() {
        super.prepareReferencesForMarshalling();

        this.p_action = null;

        this.p_user = null;

        this.p_resource = null;

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

        sb.append("\n[ ActionExecution");
        sb.append("={");

        sb.append("container=");
        if (getContainer() != null) {
            // short toString :
            MetadataElement.deepToString(sb, false, ids, getContainer());
        }
        sb.append(" | ");

        sb.append("executionDate=");
        if (getExecutionDate() != null) {
            MetadataElement.deepToString(sb, isDeep, ids, getExecutionDate());
        }
        sb.append(", ");
        sb.append("status=");
        if (getStatus() != null) {
            MetadataElement.deepToString(sb, isDeep, ids, getStatus());
        }
        sb.append(", ");
        sb.append("action.id=");
        if (getAction() != null) {
            getAction().getId();
        }
        sb.append(", ");
        sb.append("user.id=");
        if (getUser() != null) {
            getUser().getId();
        }
        sb.append(", ");
        sb.append("resource.id=");
        if (getResource() != null) {
            getResource().getId();
        }

        return sb.append("} ]");

    }

    public boolean isValid() {
        if (!super.isValid())
            return false;
        boolean isOk = true;
        isOk = isOk && executionDate != null;
        isOk = isOk && status != null;
        isOk = isOk && (action != null || p_action != null);
        isOk = isOk && (user != null || p_user != null);
        isOk = isOk && (resource != null || p_resource != null);
        return isOk;
    }

    public String validationErrors() {
        StringBuffer sb = new StringBuffer(super.validationErrors());

        if (executionDate == null)
            sb.append("- atribute 'executionDate' cannot be null\n");

        if (status == null)
            sb.append("- atribute 'status' cannot be null\n");

        if (action == null && p_action == null)
            sb.append("- reference 'action' cannot be null\n");

        if (user == null && p_user == null)
            sb.append("- reference 'user' cannot be null\n");

        if (resource == null && p_resource == null)
            sb.append("- reference 'resource' cannot be null\n");

        return sb.toString();
    }

}
