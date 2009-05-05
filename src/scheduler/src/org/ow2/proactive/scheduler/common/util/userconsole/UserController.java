/*
 * ################################################################
 *
 * ProActive: The Java(TM) library for Parallel, Distributed,
 *            Concurrent computing with Security and Mobility
 *
 * Copyright (C) 1997-2009 INRIA/University of Nice-Sophia Antipolis
 * Contact: proactive@ow2.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version
 * 2 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 * $PROACTIVE_INITIAL_DEV$
 */
package org.ow2.proactive.scheduler.common.util.userconsole;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map.Entry;

import javax.management.MBeanInfo;
import javax.management.ObjectName;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.security.auth.login.LoginException;

import org.apache.commons.cli.AlreadySelectedException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.log4j.Logger;
import org.objectweb.proactive.core.jmx.ProActiveConnection;
import org.objectweb.proactive.core.jmx.client.ClientConnector;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.objectweb.proactive.core.util.passwordhandler.PasswordField;
import org.ow2.proactive.scheduler.common.SchedulerAuthenticationInterface;
import org.ow2.proactive.scheduler.common.SchedulerConnection;
import org.ow2.proactive.scheduler.common.SchedulerStatus;
import org.ow2.proactive.scheduler.common.UserSchedulerInterface;
import org.ow2.proactive.scheduler.common.exception.SchedulerException;
import org.ow2.proactive.scheduler.common.job.Job;
import org.ow2.proactive.scheduler.common.job.JobId;
import org.ow2.proactive.scheduler.common.job.JobPriority;
import org.ow2.proactive.scheduler.common.job.JobResult;
import org.ow2.proactive.scheduler.common.job.factories.FlatJobFactory;
import org.ow2.proactive.scheduler.common.job.factories.JobFactory;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scheduler.common.util.SchedulerLoggers;
import org.ow2.proactive.scheduler.common.util.Tools;
import org.ow2.proactive.utils.console.Console;
import org.ow2.proactive.utils.console.MBeanInfoViewer;
import org.ow2.proactive.utils.console.SimpleConsole;


/**
 * UserController will help you to interact with the scheduler.<br>
 * Use this class to submit jobs, get results, pause job, etc...
 *
 * @author The ProActive Team
 * @since ProActive Scheduling 1.0
 */
public class UserController {

    private final String SCHEDULER_DEFAULT_URL = Tools.getHostURL("//localhost/");
    private final String JS_INIT_FILE = "UserActions.js";

    protected static Logger logger = ProActiveLogger.getLogger(SchedulerLoggers.CONSOLE);
    protected static final int cmdHelpMaxCharLength = 24;
    protected static final String control = "<ctl> ";

    private static final String EXCEPTIONMODE_CMD = "exMode(display,onDemand)";
    private static final String EXIT_CMD = "exit()";
    private static final String PAUSEJOB_CMD = "pausejob(id)";
    private static final String PRIORITY_CMD = "priority(id,priority)";
    private static final String RESUMEJOB_CMD = "resumejob(id)";
    private static final String KILLJOB_CMD = "killjob(id)";
    private static final String REMOVEJOB_CMD = "removejob(id)";
    private static final String SUBMIT_CMD = "submit(XMLdescriptor)";
    private static final String GET_RESULT_CMD = "result(id)";
    private static final String GET_TASK_RESULT_CMD = "tresult(id,taskName)";
    private static final String GET_OUTPUT_CMD = "output(id)";
    private static final String GET_TASK_OUTPUT_CMD = "toutput(id,taskName)";
    private static final String JMXINFO_CMD = "jmxinfo()";
    private static final String EXEC_CMD = "exec(commandFilePath)";

    private String commandName = "userScheduler";

    protected UserSchedulerInterface scheduler;
    protected boolean initialized = false;
    protected boolean terminated = false;
    protected boolean intercativeMode = false;

    protected boolean displayStack = true;
    protected boolean displayOnDemand = true;

    protected ScriptEngine engine;
    protected Console console = new SimpleConsole();

    protected MBeanInfoViewer mbeanInfoViewer;

    protected SchedulerAuthenticationInterface auth = null;
    protected CommandLine cmd = null;
    protected String user = null;
    protected String pwd = null;

    protected static UserController shell;

    /**
     * Start the Scheduler controller
     *
     * @param args the arguments to be passed
     */
    public static void main(String[] args) {
        shell = new UserController();
        shell.load(args);
    }

    public void load(String[] args) {
        Options options = new Options();

        Option help = new Option("h", "help", false, "Display this help");
        help.setRequired(false);
        options.addOption(help);

        Option username = new Option("l", "login", true, "The username to join the Scheduler");
        username.setArgName("login");
        username.setArgs(1);
        username.setRequired(false);
        options.addOption(username);

        Option schedulerURL = new Option("u", "schedulerURL", true, "The scheduler URL (default " +
            SCHEDULER_DEFAULT_URL + ")");
        schedulerURL.setArgName("schedulerURL");
        schedulerURL.setRequired(false);
        options.addOption(schedulerURL);

        addCommandLineOptions(options);

        boolean displayHelp = false;

        try {
            String pwdMsg = null;

            Parser parser = new GnuParser();
            cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                displayHelp = true;
            } else {
                String url;
                if (cmd.hasOption("u")) {
                    url = cmd.getOptionValue("u");
                } else {
                    url = SCHEDULER_DEFAULT_URL;
                }
                logger.info("Trying to connect Scheduler on " + url);
                auth = SchedulerConnection.join(url);
                logger.info("\t-> Connection established on " + url);

                logger.info("\nConnecting admin to the Scheduler");
                if (cmd.hasOption("l")) {
                    user = cmd.getOptionValue("l");
                    pwdMsg = user + "'s password: ";
                } else {
                    System.out.print("login: ");
                    BufferedReader buf = new BufferedReader(new InputStreamReader(System.in));
                    user = buf.readLine();
                    pwdMsg = "password: ";
                }

                //ask password to User
                char password[] = null;
                try {
                    password = PasswordField.getPassword(System.in, pwdMsg);
                    if (password == null) {
                        pwd = "";
                    } else {
                        pwd = String.valueOf(password);
                    }
                } catch (IOException ioe) {
                    logger.error("" + ioe);
                }

                //connect to the scheduler
                connect();
                //connect JMX service
                //connectJMXClient(URIBuilder.getHostNameFromUrl(url));
                //start the command line or the interactive mode
                start();
            }
        } catch (MissingArgumentException e) {
            logger.error(e.getLocalizedMessage());
            displayHelp = true;
        } catch (MissingOptionException e) {
            logger.error("Missing option: " + e.getLocalizedMessage());
            displayHelp = true;
        } catch (UnrecognizedOptionException e) {
            logger.error(e.getLocalizedMessage());
            displayHelp = true;
        } catch (AlreadySelectedException e) {
            logger.error(e.getClass().getSimpleName() + " : " + e.getLocalizedMessage());
            displayHelp = true;
        } catch (ParseException e) {
            displayHelp = true;
        } catch (LoginException e) {
            logger.error(e.getMessage() + "\nShutdown the controller.\n");
            System.exit(1);
        } catch (SchedulerException e) {
            logger.error(e.getMessage() + "\nShutdown the controller.\n");
            System.exit(1);
        } catch (Exception e) {
            logger.error("An error has occurred : " + e.getMessage() + "\nShutdown the controller.\n", e);
            System.exit(1);
        }

        if (displayHelp) {
            logger.info("");
            HelpFormatter hf = new HelpFormatter();
            hf.setWidth(135);
            String note = "\nNOTE : if no " + control +
                "command is specified, the controller will start in interactive mode.";
            hf.printHelp(commandName + Tools.shellExtension(), "", options, note, true);
            System.exit(2);
        }

        // if execution reaches this point this means it must exit
        System.exit(0);
    }

    protected void connect() throws LoginException {
        scheduler = auth.logAsUser(user, pwd);
        logger.info("\t-> User '" + user + "' successfully connected\n");
    }

    private void connectJMXClient(String url) {
        if (!url.startsWith("//")) {
            url = "//" + url;
        }
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        //connect the JMX client
        ClientConnector connectorClient = new ClientConnector(url, "ServerFrontend");
        try {
            connectorClient.connect();
            ProActiveConnection connection = connectorClient.getConnection();
            ObjectName mbeanName = new ObjectName("SchedulerFrontend:name=SchedulerWrapperMBean");
            MBeanInfo info = connection.getMBeanInfo(mbeanName);
            mbeanInfoViewer = new MBeanInfoViewer(connection, mbeanName, info);
        } catch (Exception e) {
            logger.error("Scheduler MBean not found using : SchedulerFrontend:name=SchedulerWrapperMBean");
        }
    }

    private void start() throws Exception {
        //start one of the two command behavior
        if (startCommandLine(cmd)) {
            startCommandListener();
        }
    }

    protected OptionGroup addCommandLineOptions(Options options) {
        OptionGroup actionGroup = new OptionGroup();

        Option opt = new Option("submit", true, control + "Submit the given job XML file");
        opt.setArgName("XMLDescriptor");
        opt.setRequired(false);
        opt.setArgs(Option.UNLIMITED_VALUES);
        actionGroup.addOption(opt);

        opt = new Option("cmd", false, control +
            "If mentionned, -submit argument becomes a command line, ie: -submit command args...");
        opt.setRequired(false);
        options.addOption(opt);
        opt = new Option("cmdf", false, control +
            "If mentionned, -submit argument becomes a text file path containing command lines to schedule");
        opt.setRequired(false);
        options.addOption(opt);
        opt = new Option("o", true, control +
            "Used with submit action, specify a log file path to store job output");
        opt.setArgName("logFile");
        opt.setRequired(false);
        opt.setArgs(1);
        options.addOption(opt);
        opt = new Option("s", true, control + "Used with submit action, specify a selection script");
        opt.setArgName("selScript");
        opt.setRequired(false);
        opt.setArgs(1);
        options.addOption(opt);
        opt = new Option("jn", true, control + "Used with submit action, specify the job name");
        opt.setArgName("jobName");
        opt.setRequired(false);
        opt.setArgs(1);
        options.addOption(opt);

        opt = new Option("pausejob", true, control + "Pause the given job (pause every non-running tasks)");
        opt.setArgName("jobId");
        opt.setRequired(false);
        opt.setArgs(1);
        actionGroup.addOption(opt);

        opt = new Option("resumejob", true, control + "Resume the given job (restart every paused tasks)");
        opt.setArgName("jobId");
        opt.setRequired(false);
        opt.setArgs(1);
        actionGroup.addOption(opt);

        opt = new Option("killjob", true, control + "Kill the given job (cause the job to finish)");
        opt.setArgName("jobId");
        opt.setRequired(false);
        opt.setArgs(1);
        actionGroup.addOption(opt);

        opt = new Option("removejob", true, control + "Remove the given job");
        opt.setArgName("jobId");
        opt.setRequired(false);
        opt.setArgs(1);
        actionGroup.addOption(opt);

        opt = new Option("result", true, control + "Get the result of the given job");
        opt.setArgName("jobId");
        opt.setRequired(false);
        opt.setArgs(1);
        actionGroup.addOption(opt);

        opt = new Option("tresult", true, control + "Get the result of the given task");
        opt.setArgName("jobId taskName");
        opt.setRequired(false);
        opt.setArgs(2);
        actionGroup.addOption(opt);

        opt = new Option("output", true, control + "Get the output of the given job");
        opt.setArgName("jobId");
        opt.setRequired(false);
        opt.setArgs(1);
        actionGroup.addOption(opt);

        opt = new Option("toutput", true, control + "Get the output of the given task");
        opt.setArgName("jobId taskName");
        opt.setRequired(false);
        opt.setArgs(2);
        actionGroup.addOption(opt);

        opt = new Option("priority", true, control +
            "Change the priority of the given job (Idle, Lowest, Low, Normal, High, Highest)");
        opt.setArgName("jobId newPriority");
        opt.setRequired(false);
        opt.setArgs(2);
        actionGroup.addOption(opt);

        //        opt = new Option("jmxinfo", false, control +
        //            "Display some statistics provided by the Scheduler MBean");
        //        opt.setRequired(false);
        //        opt.setArgs(0);
        //        actionGroup.addOption(opt);

        options.addOptionGroup(actionGroup);

        return actionGroup;
    }

    private void startCommandListener() throws Exception {
        initialize();
        console.start(" > ");
        console.printf("Type command here (type '?' or help() to see the list of commands)\n");
        String stmt;
        while (!terminated) {
            SchedulerStatus status = scheduler.getStatus();
            String prompt = "";
            if (status != SchedulerStatus.STARTED) {
                prompt = status.toString();
            }
            stmt = console.readStatement(" " + prompt + " > ");
            if ("?".equals(stmt)) {
                console.printf("\n" + helpScreen());
            } else {
                eval(stmt);
                console.printf("");
            }
        }
        console.stop();
    }

    protected boolean startCommandLine(CommandLine cmd) {
        intercativeMode = false;
        if (cmd.hasOption("pausejob")) {
            pause(cmd.getOptionValue("pausejob"));
        } else if (cmd.hasOption("resumejob")) {
            resume(cmd.getOptionValue("resumejob"));
        } else if (cmd.hasOption("killjob")) {
            kill(cmd.getOptionValue("killjob"));
        } else if (cmd.hasOption("removejob")) {
            remove(cmd.getOptionValue("removejob"));
        } else if (cmd.hasOption("submit")) {
            if (cmd.hasOption("cmd") || cmd.hasOption("cmdf")) {
                submitCMD();
            } else {
                submit(cmd.getOptionValue("submit"));
            }
        } else if (cmd.hasOption("result")) {
            result(cmd.getOptionValue("result"));
        } else if (cmd.hasOption("tresult")) {
            String[] optionValues = cmd.getOptionValues("tresult");
            if (optionValues == null || optionValues.length != 2) {
                error("tresult must have two arguments. Start with --help for more informations");
            }
            tresult(optionValues[0], optionValues[1]);
        } else if (cmd.hasOption("output")) {
            output(cmd.getOptionValue("output"));
        } else if (cmd.hasOption("toutput")) {
            String[] optionValues = cmd.getOptionValues("toutput");
            if (optionValues == null || optionValues.length != 2) {
                error("toutput must have two arguments. Start with --help for more informations");
            }
            toutput(optionValues[0], optionValues[1]);
        } else if (cmd.hasOption("priority")) {
            try {
                priority(cmd.getOptionValues("priority")[0], cmd.getOptionValues("priority")[1]);
            } catch (ArrayIndexOutOfBoundsException e) {
                printf("Missing arguments for job priority. Arguments must be <jobId> <newPriority>\n\t"
                    + "where priorities are Idle, Lowest, Low, Normal, High, Highest");
            }
        }
        //        else if (cmd.hasOption("jmxinfo")) {
        //            JMXinfo();
        //        } 
        else {
            intercativeMode = true;
            return intercativeMode;
        }
        return false;
    }

    //***************** COMMAND LISTENER *******************

    protected void handleExceptionDisplay(String msg, Throwable t) {
        if (intercativeMode) {
            if (!displayStack) {
                console.error(msg + " : " + (t.getMessage() == null ? t : t.getMessage()));
            } else {
                if (displayOnDemand) {
                    console.handleExceptionDisplay(msg, t);
                } else {
                    console.printStackTrace(t);
                }
            }
        } else {
            System.err.printf(msg + "\n");
            logger.info("", t);
        }
    }

    protected void printf(String format, Object... args) {
        if (intercativeMode) {
            console.printf(format, args);
        } else {
            System.out.printf(format + "\n", args);
        }
    }

    protected void error(String format, Object... args) {
        if (intercativeMode) {
            console.error(format, args);
        } else {
            System.err.printf(format + "\n", args);
        }
    }

    public static void setExceptionMode(boolean displayStack, boolean displayOnDemand) {
        shell.setExceptionMode_(displayStack, displayOnDemand);
    }

    private void setExceptionMode_(boolean displayStack, boolean displayOnDemand) {
        this.displayStack = displayStack;
        this.displayOnDemand = displayOnDemand;
        String msg = "Exception display mode changed : ";
        if (!displayStack) {
            msg += "stack trace not displayed";
        } else {
            if (displayOnDemand) {
                msg += "stack trace displayed on demand";
            } else {
                msg += "stack trace displayed everytime";
            }
        }
        printf(msg);
    }

    public static void help() {
        shell.help_();
    }

    private void help_() {
        printf("\n" + helpScreen());
    }

    public static String submit(String xmlDescriptor) {
        return shell.submit_(xmlDescriptor);
    }

    private String submit_(String xmlDescriptor) {
        try {
            Job job = JobFactory.getFactory().createJob(xmlDescriptor);
            JobId id = scheduler.submit(job);
            printf("Job successfully submitted ! (id=" + id.value() + ")");
            return id.value();
        } catch (Exception e) {
            handleExceptionDisplay("Error on job Submission (path=" + xmlDescriptor + ")", e);
        }
        return "";
    }

    private String submitCMD() {
        try {
            Job job;
            String jobGivenName = null;
            String jobGivenOutput = null;
            String givenSelScript = null;
            if (cmd.hasOption("jn")) {
                jobGivenName = cmd.getOptionValue("jn");
            }
            if (cmd.hasOption("o")) {
                jobGivenOutput = cmd.getOptionValue("o");
            }
            if (cmd.hasOption("s")) {
                givenSelScript = cmd.getOptionValue("s");
            }

            if (cmd.hasOption("cmd")) {
                //create job from a command to launch specified in command line
                String cmdTab[] = cmd.getOptionValues("submit");
                String jobCommand = "";

                for (String s : cmdTab) {
                    jobCommand += (s + " ");
                }
                jobCommand = jobCommand.trim();
                job = FlatJobFactory.getFactory().createNativeJobFromCommand(jobCommand, jobGivenName,
                        givenSelScript, jobGivenOutput, user);
            } else {
                String commandFilePath = cmd.getOptionValue("submit");
                job = FlatJobFactory.getFactory().createNativeJobFromCommandsFile(commandFilePath,
                        jobGivenName, givenSelScript, jobGivenOutput, user);
            }
            JobId id = scheduler.submit(job);
            printf("Job successfully submitted ! (id=" + id.value() + ")");
            return id.value();
        } catch (Exception e) {
            handleExceptionDisplay("Error on job Submission", e);
        }
        return "";
    }

    public static boolean pause(String jobId) {
        return shell.pause_(jobId);
    }

    private boolean pause_(String jobId) {
        boolean success = false;
        try {
            success = scheduler.pause(jobId).booleanValue();
        } catch (Exception e) {
            handleExceptionDisplay("Error while pausing job " + jobId, e);
            return false;
        }
        if (success) {
            printf("Job " + jobId + " paused.");
        } else {
            printf("Pause job " + jobId + " is not possible !!");
        }
        return success;
    }

    public static boolean resume(String jobId) {
        return shell.resume_(jobId);
    }

    private boolean resume_(String jobId) {
        boolean success = false;
        try {
            success = scheduler.resume(jobId).booleanValue();
        } catch (Exception e) {
            handleExceptionDisplay("Error while resuming job  " + jobId, e);
            return false;
        }
        if (success) {
            printf("Job " + jobId + " resumed.");
        } else {
            printf("Resume job " + jobId + " is not possible !!");
        }
        return success;
    }

    public static boolean kill(String jobId) {
        return shell.kill_(jobId);
    }

    private boolean kill_(String jobId) {
        boolean success = false;
        try {
            success = scheduler.kill(jobId).booleanValue();
        } catch (Exception e) {
            handleExceptionDisplay("Error while killing job  " + jobId, e);
            return false;
        }
        if (success) {
            printf("Job " + jobId + " killed.");
        } else {
            printf("kill job " + jobId + " is not possible !!");
        }
        return success;
    }

    public static void remove(String jobId) {
        shell.remove_(jobId);
    }

    private void remove_(String jobId) {
        try {
            scheduler.remove(jobId);
            printf("Job " + jobId + " removed.");
        } catch (Exception e) {
            handleExceptionDisplay("Error while removing job  " + jobId, e);
        }
    }

    public static JobResult result(String jobId) {
        return shell.result_(jobId);
    }

    private JobResult result_(String jobId) {
        try {
            JobResult result = scheduler.getJobResult(jobId);

            if (result != null) {
                printf("Job " + jobId + " result => \n");

                for (Entry<String, TaskResult> e : result.getAllResults().entrySet()) {
                    TaskResult tRes = e.getValue();

                    try {
                        printf("\t " + e.getKey() + " : " + tRes.value());
                    } catch (Throwable e1) {
                        handleExceptionDisplay("\t " + e.getKey() + " : " + tRes.getException().getMessage(),
                                e1);
                    }
                }
            } else {
                printf("Job " + jobId + " is not finished or unknown !");
            }
            return result;
        } catch (Exception e) {
            handleExceptionDisplay("Error on job " + jobId, e);
            return null;
        }
    }

    public static TaskResult tresult(String jobId, String taskName) {
        return shell.tresult_(jobId, taskName);
    }

    private TaskResult tresult_(String jobId, String taskName) {
        try {
            TaskResult result = scheduler.getTaskResult(jobId, taskName);

            if (result != null) {
                printf("Task " + taskName + " result => \n");

                try {
                    printf("\t " + result.value());
                } catch (Throwable e1) {
                    handleExceptionDisplay("\t " + result.getException().getMessage(), e1);
                }
            } else {
                printf("Task '" + taskName + "' is still running !");
            }
            return result;
        } catch (Exception e) {
            handleExceptionDisplay("Error on task " + taskName, e);
            return null;
        }
    }

    public static void output(String jobId) {
        shell.output_(jobId);
    }

    private void output_(String jobId) {
        try {
            JobResult result = scheduler.getJobResult(jobId);

            if (result != null) {
                printf("Job " + jobId + " output => \n");

                for (Entry<String, TaskResult> e : result.getAllResults().entrySet()) {
                    TaskResult tRes = e.getValue();

                    try {
                        printf(e.getKey() + " : \n" + tRes.getOutput().getAllLogs(false));
                    } catch (Throwable e1) {
                        error("", tRes.getException());
                    }
                }
            } else {
                printf("Job " + jobId + " is not finished or unknown !");
            }
        } catch (Exception e) {
            handleExceptionDisplay("Error on job " + jobId, e);
        }
    }

    public static void toutput(String jobId, String taskName) {
        shell.toutput_(jobId, taskName);
    }

    private void toutput_(String jobId, String taskName) {
        try {
            TaskResult result = scheduler.getTaskResult(jobId, taskName);

            if (result != null) {
                try {
                    printf(taskName + " output :");
                    printf(result.getOutput().getAllLogs(false));
                } catch (Throwable e1) {
                    handleExceptionDisplay("\t " + result.getException().getMessage(), e1);
                }
            } else {
                printf("Task '" + taskName + "' is still running !");
            }
        } catch (Exception e) {
            handleExceptionDisplay("Error on task " + taskName, e);
        }
    }

    public static void priority(String jobId, String newPriority) {
        shell.priority_(jobId, newPriority);
    }

    private void priority_(String jobId, String newPriority) {
        try {
            JobPriority prio = JobPriority.findPriority(newPriority);
            scheduler.changePriority(jobId, prio);
            printf("Job " + jobId + " priority changed to '" + prio + "' !");
        } catch (Exception e) {
            handleExceptionDisplay("Error on job " + jobId, e);
        }
    }

    public static void JMXinfo() {
        shell.JMXinfo_();
    }

    private void JMXinfo_() {
        try {
            printf(mbeanInfoViewer.getInfo());
        } catch (Exception e) {
            handleExceptionDisplay("Error while retrieving JMX informations", e);
        }
    }

    public static void exec(String commandFilePath) {
        shell.exec_(commandFilePath);
    }

    private void exec_(String commandFilePath) {
        try {
            File f = new File(commandFilePath.trim());
            BufferedReader br = new BufferedReader(new FileReader(f));
            eval(readFileContent(br));
            br.close();
        } catch (Exception e) {
            handleExceptionDisplay("*ERROR*", e);
        }
    }

    public static void exit() {
        shell.exit_();
    }

    private void exit_() {
        console.printf("Exiting controller.");
        try {
            scheduler.disconnect();
        } catch (Exception e) {
        }
        terminated = true;
    }

    public static UserSchedulerInterface getUserScheduler() {
        return shell.getUserScheduler_();
    }

    private UserSchedulerInterface getUserScheduler_() {
        return scheduler;
    }

    //***************** OTHER *******************

    protected void initialize() throws IOException {
        if (!initialized) {
            ScriptEngineManager manager = new ScriptEngineManager();
            // Engine selection
            engine = manager.getEngineByExtension("js");
            initialized = true;
            //read and launch Action.js
            BufferedReader br = new BufferedReader(new InputStreamReader(UserController.class
                    .getResourceAsStream(JS_INIT_FILE)));
            eval(readFileContent(br));
        }
    }

    protected void eval(String cmd) {
        try {
            if (!initialized) {
                initialize();
            }
            //Evaluate the command
            if (cmd == null) {
                console.error("*ERROR* - Standard input stream has been terminated !");
                terminated = true;
            } else {
                engine.eval(cmd);
            }
        } catch (ScriptException e) {
            console.error("*SYNTAX ERROR* - " + format(e.getMessage()));
        } catch (Exception e) {
            handleExceptionDisplay("Error while evaluating command", e);
        }
    }

    private static String format(String msg) {
        msg = msg.replaceFirst("[^:]+:", "");
        return msg.replaceFirst("[(]<.*", "").trim();
    }

    protected static String readFileContent(BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        String tmp;
        while ((tmp = reader.readLine()) != null) {
            sb.append(tmp);
        }
        return sb.toString();
    }

    //***************** HELP SCREEN *******************

    protected String helpScreen() {
        StringBuilder out = new StringBuilder("Scheduler controller commands are :\n\n");

        out
                .append(String
                        .format(
                                " %1$-24s Change the way exceptions are displayed (if display is true, stacks are displayed - if onDemand is true, prompt before displaying stacks)\n\n",
                                EXCEPTIONMODE_CMD));
        out.append(String.format(
                " %1$-24s Pause the given job (parameter is an int or a string representing the jobId)\n",
                PAUSEJOB_CMD));
        out.append(String.format(
                " %1$-24s Resume the given job (parameter is an int or a string representing the jobId)\n",
                RESUMEJOB_CMD));
        out.append(String.format(
                " %1$-24s Kill the given job (parameter is an int or a string representing the jobId)\n",
                KILLJOB_CMD));
        out.append(String
                .format(
                        " %1$-24s Change the priority of the given job (parameters are an int or a string representing the jobId "
                            + "AND a string representing the new priority)\n"
                            + " %2$-24s Priorities are Idle, Lowest, Low, Normal, High, Highest\n",
                        PRIORITY_CMD, " "));
        out
                .append(String
                        .format(
                                " %1$-24s Remove the given job from the Scheduler (parameter is an int or a string representing the jobId)\n",
                                REMOVEJOB_CMD));
        out
                .append(String
                        .format(
                                " %1$-24s Get the result of the given job (parameter is an int or a string representing the jobId)\n",
                                GET_RESULT_CMD));
        out
                .append(String
                        .format(
                                " %1$-24s Get the result of the given task (parameter is an int or a string representing the jobId, and the task name)\n",
                                GET_TASK_RESULT_CMD));
        out
                .append(String
                        .format(
                                " %1$-24s Get the output of the given job (parameter is an int or a string representing the jobId)\n",
                                GET_OUTPUT_CMD));
        out
                .append(String
                        .format(
                                " %1$-24s Get the output of the given task (parameter is an int or a string representing the jobId, and the task name)\n",
                                GET_TASK_OUTPUT_CMD));
        out
                .append(String
                        .format(
                                " %1$-24s Submit a new job (parameter is a string representing the job XML descriptor URL)\n",
                                SUBMIT_CMD));
        //        out.append(String.format(" %1$-24s Display some statistics provided by the Scheduler MBean\n",
        //                JMXINFO_CMD));
        out
                .append(String
                        .format(
                                " %1$-24s Execute the content of the given script file (parameter is a string representing a command-file path)\n",
                                EXEC_CMD));
        out.append(String.format(" %1$-24s Exit Scheduler controller\n", EXIT_CMD));

        return out.toString();
    }

    /**
     * Set the commandName value to the given commandName value
     *
     * @param commandName the commandName to set
     */
    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

}
