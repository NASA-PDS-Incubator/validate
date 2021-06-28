//Copyright © 2019, California Institute of Technology ("Caltech").
// U.S. Government sponsorship acknowledged.
//
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// • Redistributions of source code must retain the above copyright notice,
//   this list of conditions and the following disclaimer.
// • Redistributions must reproduce the above copyright notice, this list of
//   conditions and the following disclaimer in the documentation and/or other
//   materials provided with the distribution.
// • Neither the name of Caltech nor its operating division, the Jet Propulsion
//   Laboratory, nor the names of its contributors may be used to endorse or
//   promote products derived from this software without specific prior written
//   permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package gov.nasa.pds.validate.commandline.options;

import org.apache.commons.cli.Options;

/**
 * Class that builds the command-line options.
 *
 * @author mcayanan
 *
 */
public class FlagOptions {
    /** Holds a list of valid options. */
    private static Options options;

    static {
        options = new Options();

        options.addOption(new ToolsOption(Flag.ALTERNATE_FILE_PATHS));
        options.addOption(new ToolsOption(Flag.BASE_PATH));
        options.addOption(new ToolsOption(Flag.CATALOG));
        options.addOption(new ToolsOption(Flag.CHECKSUM_MANIFEST));
        options.addOption(new ToolsOption(Flag.CONFIG));
        options.addOption(new ToolsOption(Flag.MAX_ERRORS));
        options.addOption(new ToolsOption(Flag.REGEXP));
        options.addOption(new ToolsOption(Flag.HELP));
        options.addOption(new ToolsOption(Flag.REPORT));
        options.addOption(new ToolsOption(Flag.TARGET));
        options.addOption(new ToolsOption(Flag.VERBOSE));
        options.addOption(new ToolsOption(Flag.SCHEMA));
        options.addOption(new ToolsOption(Flag.SCHEMATRON));
        options.addOption(new ToolsOption(Flag.LOCAL));
        options.addOption(new ToolsOption(Flag.VERSION));
        options.addOption(new ToolsOption(Flag.STYLE));
        options.addOption(new ToolsOption(Flag.RULE));
        options.addOption(new ToolsOption(Flag.SKIP_CONTENT_VALIDATION));
        options.addOption(new ToolsOption(Flag.SKIP_CONTEXT_REFERENCE_CHECK));
        options.addOption(new ToolsOption(Flag.CHECK_INBETWEEN_FIELDS));
        options.addOption(new ToolsOption(Flag.NO_DATA));
        options.addOption(new ToolsOption(Flag.SPOT_CHECK_DATA));
        options.addOption(new ToolsOption(Flag.ALLOW_UNLABELED_FILES));
        options.addOption(new ToolsOption(Flag.LATEST_JSON_FILE));
        options.addOption(new ToolsOption(Flag.NONREGPROD_JSON_FILE));
        options.addOption(new ToolsOption(Flag.SKIP_CONTEXT_VALIDATION));
        options.addOption(new ToolsOption(Flag.TARGET_MANIFEST));
        options.addOption(new ToolsOption(Flag.SKIP_PRODUCT_VALIDATION));
        /** DEPRECATED Options **/
        options.addOption(new ToolsOption(Flag.FORCE));
        options.addOption(new ToolsOption(Flag.MODEL));
    }

    /**
     * Get the list of options.
     *
     * @return A list of options.
     */
    public static Options getOptions() {
        return options;
    }
}
