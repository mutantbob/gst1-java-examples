package org.freedesktop.gstreamer.examples;

import org.freedesktop.gstreamer.*;

public class TooMuchPlumbing
{
    public static void main(String[] argv)
    {
        Gst.init();

        mission1(Support.srcVideo);

        Gst.main();
    }

    public static void mission1(String srcVideo)
    {
        Pipeline pipe = new Pipeline();
        Element dec;
        if (false) {
            Bin sources = Bin.launch("filesrc name=src ! decodebin name=dec", false);

            Element fs = sources.getElementByName("src");
            fs.set("location", srcVideo);

            // XXX this won't work because dec is inside a bin and the sometimes pads can't be linked to an element in a different bin
            dec = sources.getElementByName("dec");

            pipe.add(sources);
        } else {
            Element fs = ElementFactory.make("filesrc", "moo");
            fs.set("location", srcVideo);

            dec = ElementFactory.make("decodebin", "dec");
            pipe.addMany(fs, dec);
            Pipeline.linkMany(fs, dec);
            //pipe.link(fs, dec);
        }

        pipe.debugToDotFile(Bin.DEBUG_GRAPH_SHOW_ALL, "plumbing");

        Element mux = ElementFactory.make("mpegtsmux", "mux");
        Element fileSink = ElementFactory.make("filesink", "filesink");
        fileSink.set("location", "/tmp/test.ts");

        pipe.addMany(mux, fileSink);
        Pipeline.linkMany(mux, fileSink);

        pipe.debugToDotFile(Bin.DEBUG_GRAPH_SHOW_ALL, "plumbing");

        dec.connect(new ConnectSometimesPads(pipe, mux));

        Support.addDebuggingBusListeners(pipe.getBus());
        Support.rigQuitOnEOS(pipe.getBus());

        pipe.getBus().connect((source, old, current, pending) -> System.out.println("STATE CHANGD "+source+" "+old+" "+current+" "+pending));

        pipe.play();

        pipe.debugToDotFile(Bin.DEBUG_GRAPH_SHOW_ALL, "plumbing");
    }

    private static class ConnectSometimesPads
        implements Element.PAD_ADDED
    {
        private final Pipeline pipe;
        private final Element mux;
        int padCount;

        public ConnectSometimesPads(Pipeline pipe, Element mux)
        {
            this.pipe = pipe;
            this.mux = mux;
            padCount = 0;
        }

        @Override
        public void padAdded(Element element, Pad pad)
        {
            System.out.println("pad added "+pad.getName()+" "+pad.getTypeName());
            System.out.println("allowed = "+ Support.capsReport(pad.getAllowedCaps()));
            System.out.println("caps = "+ Support.capsReport(pad.getCaps()));
            System.out.println("negotiated = "+ Support.capsReport(pad.getNegotiatedCaps()));

            Caps caps = pad.getCaps();
            if (caps.getStructure(0).getName().startsWith("video")) {
                connectVideoReencode(pad);
            } else {
                Element trash = ElementFactory.make("fakesink", "fake");
                pipe.add(trash);
                PadLinkReturn stat1 = pad.link(trash.getSinkPads().get(0));
                System.out.println("trash pad link "+stat1);
            }

            pipe.debugToDotFile(Bin.DEBUG_GRAPH_SHOW_ALL, "plumbing");
        }

        public void connectVideoReencode(Pad pad)
        {
            Bin venc = Bin.launch("videoconvert name=sink " +
                    "! videorate ! video/x-raw,framerate=30/1 " +
                    "! x264enc pass=4 quantizer=21 ", true);

            pipe.add(venc);

            Element sink = venc.getElementByName("sink");
            Pad pad2 = venc.getSinkPads().get(0);
            System.out.println(pad2.isLinked() +"\t"+pad.isLinked());
            PadLinkReturn stat1 = pad.link(pad2);
            //pad2.link(pad);
            System.out.println("link stat:\t"+ stat1);

            Pad pad3 = venc.getSrcPads().get(0);
            Pad pad4 = mux.getRequestPad("sink_"+(padCount++));
            //pad4.link(pad3);
            PadLinkReturn stat2 = pad3.link(pad4);
            System.out.println("link stat:\t"+ stat2);

            System.out.println("encoder sink caps = "+ Support.capsReport(pad2.getNegotiatedCaps()));
            System.out.println("mux sink caps = "+ Support.capsReport(pad4.getNegotiatedCaps()));

        }
    }
}
