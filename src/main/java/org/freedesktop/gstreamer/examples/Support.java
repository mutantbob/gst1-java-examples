package org.freedesktop.gstreamer.examples;

import org.freedesktop.gstreamer.*;

public class Support
{
    public static String srcVideo = "/home/thoth/art/subnautica/windows/2018-02-04 18-54-51.ts";

    public static void waitForEOS(Bus bus)
    {
        boolean[] done = {false};

        synchronized(done) {
            bus.connect((Bus.EOS) gstObject -> {
                synchronized (done) {
                    done[0] = true;
                    done.notifyAll();
                }
            });

            while (! done[0]) {
                try {
                    done.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void rigQuitOnEOS(Bus bus)
    {
        bus.connect((Bus.EOS) obj -> Gst.quit() );
    }

    public static void addDebuggingBusListeners(Bus bus)
    {
        bus.connect((Bus.EOS) gstObject -> System.out.println("EOS"));

        bus.connect((Bus.ERROR) (gstObject, i, s) -> System.out.println("ERROR "+i+" "+s+" "+gstObject));

        bus.connect((Bus.WARNING) (gstObject, i, s) -> System.out.println("WARN "+i+" "+s+" "+gstObject));
    }

    public static String capsReport(Caps caps)
    {
        if (caps==null)
            return "null";
        StringBuilder rval = new StringBuilder("["+caps.size()+"]");
        for (int i=0; i<caps.size(); i++) {
            rval.append(" "+caps.getStructure(i).getName());
        }
        return rval.toString();
    }
}
