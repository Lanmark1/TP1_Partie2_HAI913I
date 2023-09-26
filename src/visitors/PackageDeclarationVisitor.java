package visitors;

import java.util.ArrayList; 
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.PackageDeclaration;

public class PackageDeclarationVisitor extends ASTVisitor {

List<PackageDeclaration> packages = new ArrayList<>();
	
	public boolean visit(PackageDeclaration node) {
		packages.add(node);
		return super.visit(node);
	}
	
	public List<PackageDeclaration> getPackages() {
		return packages;
	}

}
