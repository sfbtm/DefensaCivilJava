package com.defensacivil.dto;

public class ActionDTO {
    private int id;
    private String description;
    private String stage;
    private Integer member_id;
    private MemberInfo member;

    public static class MemberInfo {
        private String names;
        private String last_names;

        public MemberInfo() {
        }

        public MemberInfo(String names, String last_names) {
            this.names = names;
            this.last_names = last_names;
        }

        public String getNames() {
            return names;
        }

        public void setNames(String names) {
            this.names = names;
        }

        public String getLast_names() {
            return last_names;
        }

        public void setLast_names(String last_names) {
            this.last_names = last_names;
        }
    }

    public ActionDTO() {
    }

    public ActionDTO(int id, String description, String stage, Integer member_id, MemberInfo member) {
        this.id = id;
        this.description = description;
        this.stage = stage;
        this.member_id = member_id;
        this.member = member;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public Integer getMember_id() {
        return member_id;
    }

    public void setMember_id(Integer member_id) {
        this.member_id = member_id;
    }

    public MemberInfo getMember() {
        return member;
    }

    public void setMember(MemberInfo member) {
        this.member = member;
    }
}
