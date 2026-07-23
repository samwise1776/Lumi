const vscode = require("vscode");

function shellQuote(value) {
  if (process.platform === "win32") {
    return `"${value.replace(/"/g, '""')}"`;
  }
  return `'${value.replace(/'/g, `'\\''`)}'`;
}

function activate(context) {
  const runFile = vscode.commands.registerCommand("lumi.runFile", async () => {
    const editor = vscode.window.activeTextEditor;

    if (!editor || editor.document.languageId !== "lumi") {
      vscode.window.showErrorMessage("Open a .lumi file before running Lumi.");
      return;
    }

    if (editor.document.isDirty) {
      await editor.document.save();
    }

    const terminal =
      vscode.window.terminals.find((item) => item.name === "Lumi") ||
      vscode.window.createTerminal("Lumi");

    terminal.show();
    terminal.sendText(`lumi run ${shellQuote(editor.document.fileName)}`, true);
  });

  const openIde = vscode.commands.registerCommand("lumi.openIde", () => {
    const terminal =
      vscode.window.terminals.find((item) => item.name === "Lumi") ||
      vscode.window.createTerminal("Lumi");
    terminal.sendText("lumi --ide", true);
  });

  context.subscriptions.push(runFile, openIde);
}

function deactivate() {}

module.exports = { activate, deactivate };
