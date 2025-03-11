package org.sciserver.springapp.racm.ugm.domain;

import static java.util.stream.Collectors.collectingAndThen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sciserver.racm.resourcecontext.model.RegisteredResourceModel;
import org.sciserver.racm.ugm.model.GroupAccessibility;
import org.sciserver.racm.ugm.model.GroupInfo;
import org.sciserver.racm.ugm.model.GroupRole;
import org.sciserver.racm.ugm.model.MemberGroupModel;
import org.sciserver.racm.ugm.model.MemberStatus;
import org.sciserver.racm.ugm.model.MemberUserModel;
import org.sciserver.racm.ugm.model.PersonalUserInfo;
import org.sciserver.racm.ugm.model.ServiceAccountModel;
import org.sciserver.racm.ugm.model.UserInfo;

import edu.jhu.rac.AssociatedSciEntity;
import edu.jhu.rac.OwnershipCategory;
import edu.jhu.rac.Resource;
import edu.jhu.user.Member;
import edu.jhu.user.ServiceAccount;
import edu.jhu.user.User;
import edu.jhu.user.UserGroup;
import edu.jhu.user.UserVisibility;

/**
 * This class has static methods to map between vo-urp generated classes and
 * classes in the racm.ugm.models package. Main reason to have these methods
 * *not* on the models classes themselves is so that they can be exported in a
 * jar file for reuse by clients of RACM. The
 * 
 * @author gerard
 *
 */
public class UGMModelsMapper {

    private static final Set<String> validVisibilities = Stream.of(UserVisibility.values()).map(UserVisibility::name)
            .collect(collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));

    public static MemberUserModel map(Member member) {
        MemberUserModel mum = new MemberUserModel();
        if (member.getScisEntity() instanceof User)
            mum.setUser(map((User) member.getScisEntity()));
        mum.setStatus(MemberStatus.fromValue(member.getStatus().value()));
        mum.setRole(GroupRole.valueOf(member.getMemberRole().value()));
        return mum;
    }

    /**
     * maps UserGroup as Member of a group.
     * 
     * @param member
     * @return
     */
    public static MemberGroupModel mapMemberGroup(Member member) {
        MemberGroupModel mgm = new MemberGroupModel();
        mgm.setGroup(map((UserGroup) member.getScisEntity(), false)); // false, do not build members for child group

        return mgm;
    }

    public static UserInfo map(User u) {
        UserInfo ui = new UserInfo();
        ui.setUsername(u.getUsername());
        ui.setId(u.getId());
        if (u.getParty() != null) {
            ui.setAffiliation(u.getParty().getAffiliation());
            ui.setFullname(u.getParty().getFullName());
        }

        return ui;
    }

    public static GroupInfo map(UserGroup ug, boolean fullDetails) {
        GroupInfo gi = new GroupInfo();
        gi.setGroupName(ug.getName());
        gi.setId(ug.getId());
        gi.setAccessibility(GroupAccessibility.valueOf(ug.getAccessibility().name()));
        if (fullDetails) {
            gi.setOwner(map(ug.getOwner()));
            gi.setDescription(ug.getDescription());
            fillMembers(gi, ug);
        }
        return gi;
    }

    /**
     * Map a ServiceAccount to a ServiceAccountModel<br/>
     * 
     * @param sa
     * @return
     */
    public static ServiceAccountModel map(ServiceAccount sa) {
        ServiceAccountModel sam = new ServiceAccountModel(sa.getId(), sa.getPublisherDID());
        return sam;
    }

    /**
     * add members and invitations to this GroupInfo.
     * 
     * @param ug
     */
    private static void fillMembers(GroupInfo gi, UserGroup ug) {
        ArrayList<MemberUserModel> memberUsers = new ArrayList<MemberUserModel>();
        ArrayList<MemberGroupModel> memberGroups = new ArrayList<>();
        for (Member m : ug.getMember()) {
            edu.jhu.user.SciserverEntity se = m.getScisEntity();
            if (se instanceof User) {
                MemberUserModel mm = map(m);
                memberUsers.add(mm);
            } else if (se instanceof UserGroup) {
                MemberGroupModel mg = mapMemberGroup(m);
                memberGroups.add(mg);
            }
        }
        gi.setMemberGroups(memberGroups);
        gi.setMemberUsers(memberUsers);
    }

    public static GroupInfo map(UserGroup ug) {
        return map(ug, true);
    }

    public static PersonalUserInfo mapPersonalInfo(User user) {
        PersonalUserInfo pui = new PersonalUserInfo(user.getId(), user.getUsername(), user.getContactEmail(),
                user.getPreferences(), null, null, map(user.getVisibility()));
        edu.jhu.user.Party p = user.getParty();
        if (p != null) {
            pui.setFullname(p.getFullName());
            pui.setAffiliation(p.getAffiliation());
        }
        return pui;
    }

    private static String map(UserVisibility uv) {
        String visibility = uv.value();
        if (!validVisibilities.contains(visibility))
            throw new IllegalStateException(String.format("Invalid visibility: %s", uv));
        return visibility;
    }

}
