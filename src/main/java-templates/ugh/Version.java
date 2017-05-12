package ugh;

public class Version {
    public final static String VERSION = "${project.version}";
    public final static String BUILDVERSION = "${project.artifactId}-${project.version}";
    public final static String BUILDDATE = "${timestamp}";
    public static String PROGRAMNAME = "Kitodo";
}
